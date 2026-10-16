-- Run after all existing migrations, including the previously future-dated random-bytes fix.
create extension if not exists btree_gist with schema extensions;
set local search_path=public,extensions;

alter table public.time_slots add constraint no_overlapping_pitch_slots
exclude using gist (
  pitch_id with =,
  tsrange(slot_date + starts_at, slot_date + ends_at, '[)') with &&
);

create table public.staff_pitch_assignments (
  user_id uuid not null references public.profiles(id) on delete cascade,
  pitch_id text not null references public.pitches(id) on delete cascade,
  primary key (user_id, pitch_id)
);
create index staff_assignments_by_pitch on public.staff_pitch_assignments(pitch_id);
alter table public.staff_pitch_assignments enable row level security;
revoke all on public.staff_pitch_assignments from anon, authenticated;
grant all on public.staff_pitch_assignments to service_role;

create function public.can_manage_pitch(p_user_id uuid, p_pitch_id text)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.profiles p where p.id = p_user_id and
      (p.role = 'admin' or (p.role = 'staff' and exists (
        select 1 from public.staff_pitch_assignments a
        where a.user_id = p_user_id and a.pitch_id = p_pitch_id
      )))
  );
$$;
revoke all on function public.can_manage_pitch(uuid, text) from public, anon, authenticated;
grant execute on function public.can_manage_pitch(uuid, text) to service_role;

create function public.assign_staff_pitches(p_actor_id uuid,p_staff_id uuid,p_pitch_ids text[])
returns void language plpgsql security definer set search_path='' as $$
begin
 if not exists(select 1 from public.profiles where id=p_actor_id and role='admin') then raise exception 'permission_denied'; end if;
 perform id from public.profiles where id=p_staff_id and role='staff' for update;
 if not found then raise exception 'staff_not_found'; end if;
 if exists(select 1 from unnest(p_pitch_ids) x where not exists(select 1 from public.pitches p where p.id=x))
 then raise exception 'pitch_not_found'; end if;
 delete from public.staff_pitch_assignments where user_id=p_staff_id;
 insert into public.staff_pitch_assignments(user_id,pitch_id) select p_staff_id,x from unnest(p_pitch_ids) x on conflict do nothing;
end;
$$;
revoke all on function public.assign_staff_pitches(uuid,uuid,text[]) from public,anon,authenticated;
grant execute on function public.assign_staff_pitches(uuid,uuid,text[]) to service_role;

create or replace function public.create_booking(
 p_user_id uuid, p_pitch_id text, p_slot_id text, p_booking_date date,
 p_customer_name text, p_customer_phone text, p_payment_method text
) returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare
 s public.time_slots%rowtype; p public.pitches%rowtype; b public.bookings%rowtype;
 local_now timestamp := timezone('Asia/Ho_Chi_Minh', now());
begin
 if p_booking_date < local_now::date then raise exception 'booking_date_in_past'; end if;
 if p_booking_date > local_now::date + 7 then raise exception 'booking_date_too_far'; end if;
 select * into s from public.time_slots where id=p_slot_id and pitch_id=p_pitch_id
   and slot_date=p_booking_date for update;
 if not found then raise exception 'slot_not_found'; end if;
 if s.slot_date + s.starts_at <= local_now then raise exception 'slot_in_past'; end if;
 if s.status <> 'available' then raise exception 'slot_unavailable'; end if;
 select * into p from public.pitches where id=p_pitch_id and is_active;
 if not found then raise exception 'pitch_not_found'; end if;
 insert into public.bookings(booking_code,user_id,pitch_id,slot_id,booking_date,pitch_name,pitch_address,
   time_range,total_price,customer_name,customer_phone,payment_method,payment_status)
 values('SB-' || upper(encode(extensions.gen_random_bytes(6),'hex')),p_user_id,p_pitch_id,p_slot_id,
   p_booking_date,p.name,p.address,to_char(s.starts_at,'HH24:MI') || ' - ' || to_char(s.ends_at,'HH24:MI'),
   s.price,p_customer_name,p_customer_phone,p_payment_method,
   case when p_payment_method='cash' then 'cash_due' else 'qr_due' end)
 returning * into b;
 update public.time_slots set status='booked' where id=s.id;
 return next b;
exception when unique_violation then raise exception 'slot_unavailable';
end;
$$;

create or replace function public.cancel_booking(p_user_id uuid,p_booking_id uuid)
returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare b public.bookings%rowtype; s public.time_slots%rowtype;
begin
 select * into b from public.bookings where id=p_booking_id and user_id=p_user_id for update;
 if not found then raise exception 'booking_not_found'; end if;
 if b.status='cancelled' then return next b; return; end if;
 select * into s from public.time_slots where id=b.slot_id for update;
 if s.slot_date + s.starts_at <= timezone('Asia/Ho_Chi_Minh',now()) then
   raise exception 'booking_not_cancellable'; end if;
 update public.bookings set status='cancelled',cancelled_at=now() where id=b.id returning * into b;
 update public.time_slots set status='available' where id=s.id and status='booked';
 return next b;
end;
$$;

create or replace function public.reschedule_booking(p_user_id uuid,p_booking_id uuid,p_slot_id text,p_booking_date date)
returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare b public.bookings%rowtype; old_s public.time_slots%rowtype; new_s public.time_slots%rowtype;
 local_now timestamp := timezone('Asia/Ho_Chi_Minh',now());
begin
 select * into b from public.bookings where id=p_booking_id and user_id=p_user_id for update;
 if not found or b.status<>'confirmed' then raise exception 'booking_not_found'; end if;
 if p_booking_date < local_now::date then raise exception 'booking_date_in_past'; end if;
 if p_booking_date > local_now::date+7 then raise exception 'booking_date_too_far'; end if;
 -- Deterministic lock order prevents reciprocal reschedules from deadlocking.
 perform id from public.time_slots where id in (b.slot_id,p_slot_id) order by id for update;
 select * into old_s from public.time_slots where id=b.slot_id;
 if old_s.slot_date + old_s.starts_at <= local_now then raise exception 'booking_not_cancellable'; end if;
 select * into new_s from public.time_slots where id=p_slot_id and pitch_id=b.pitch_id and slot_date=p_booking_date;
 if not found then raise exception 'slot_not_found'; end if;
 if new_s.id=old_s.id then return next b; return; end if;
 if new_s.slot_date + new_s.starts_at <= local_now then raise exception 'slot_in_past'; end if;
 if new_s.status<>'available' then raise exception 'slot_unavailable'; end if;
 if not exists(select 1 from public.pitches where id=b.pitch_id and is_active) then raise exception 'pitch_not_found'; end if;
 update public.time_slots set status='available' where id=old_s.id;
 update public.time_slots set status='booked' where id=new_s.id;
 update public.bookings set slot_id=new_s.id,booking_date=p_booking_date,
   time_range=to_char(new_s.starts_at,'HH24:MI') || ' - ' || to_char(new_s.ends_at,'HH24:MI'),
   total_price=new_s.price,rescheduled_at=now() where id=b.id returning * into b;
 return next b;
end;
$$;

create function public.admin_booking_status(p_actor_id uuid,p_booking_id uuid,p_status text)
returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare b public.bookings%rowtype; s public.time_slots%rowtype;
begin
 select * into b from public.bookings where id=p_booking_id for update;
 if not found then raise exception 'booking_not_found'; end if;
 if not public.can_manage_pitch(p_actor_id,b.pitch_id) then raise exception 'permission_denied'; end if;
 if p_status not in ('confirmed','cancelled') then raise exception 'invalid_status'; end if;
 if b.status=p_status then return next b; return; end if;
 select * into s from public.time_slots where id=b.slot_id for update;
 if s.slot_date+s.starts_at <= timezone('Asia/Ho_Chi_Minh',now()) then raise exception 'booking_not_cancellable'; end if;
 if p_status='confirmed' then
   if s.status<>'available' then raise exception 'slot_unavailable'; end if;
   if not exists(select 1 from public.pitches where id=b.pitch_id and is_active) then raise exception 'pitch_not_found'; end if;
   if b.booking_date > timezone('Asia/Ho_Chi_Minh',now())::date+7 then raise exception 'booking_date_too_far'; end if;
   update public.time_slots set status='booked' where id=s.id;
 else
   update public.time_slots set status='available' where id=s.id and status='booked';
 end if;
 update public.bookings set status=p_status,cancelled_at=case when p_status='cancelled' then now() else null end
 where id=b.id returning * into b;
 return next b;
exception when unique_violation then raise exception 'slot_unavailable';
end;
$$;

create function public.admin_booking_payment(p_actor_id uuid,p_booking_id uuid,p_payment_status text)
returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare b public.bookings%rowtype;
begin
 select * into b from public.bookings where id=p_booking_id for update;
 if not found then raise exception 'booking_not_found'; end if;
 if not public.can_manage_pitch(p_actor_id,b.pitch_id) then raise exception 'permission_denied'; end if;
 if b.status<>'confirmed' then raise exception 'booking_not_cancellable'; end if;
 if p_payment_status not in ('paid','cash_due','qr_due') then raise exception 'invalid_status'; end if;
 if (p_payment_status='cash_due' and b.payment_method<>'cash') or
    (p_payment_status='qr_due' and b.payment_method<>'qr') then raise exception 'invalid_status'; end if;
 update public.bookings set payment_status=p_payment_status where id=b.id returning * into b;
 return next b;
end;
$$;
revoke all on function public.admin_booking_status(uuid,uuid,text),public.admin_booking_payment(uuid,uuid,text)
 from public,anon,authenticated;
grant execute on function public.admin_booking_status(uuid,uuid,text),public.admin_booking_payment(uuid,uuid,text) to service_role;

create function public.sync_profile_auth_email() returns trigger
language plpgsql security definer set search_path='' as $$
begin
 update public.profiles set email=new.email,updated_at=now() where id=new.id;
 return new;
end;
$$;
revoke all on function public.sync_profile_auth_email() from public,anon,authenticated;
create trigger auth_email_to_profile after update of email on auth.users
for each row when(old.email is distinct from new.email) execute function public.sync_profile_auth_email();
-- Email is an Auth identity; contact phone remains editable profile data.
revoke update(email) on public.profiles from authenticated;
