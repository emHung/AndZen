-- Customer-owned data is accessed with the caller JWT. Elevated server operations
-- remain reserved for admin workflows and account deletion.

grant select on public.profiles to authenticated;
revoke update on public.profiles from authenticated;
grant update (full_name, phone, avatar_path, avatar_url, gender, birthdate, district, updated_at)
  on public.profiles to authenticated;
revoke update (role, email, id, created_at) on public.profiles from authenticated;

grant select on public.pitches, public.time_slots to anon, authenticated;
grant select, insert, delete on public.favorites to authenticated;
grant select, insert, update on public.user_settings to authenticated;
grant select on public.bookings to authenticated;

drop policy if exists pitches_read_active on public.pitches;
create policy pitches_read_active on public.pitches
  for select to anon, authenticated using (is_active);

drop policy if exists slots_read on public.time_slots;
create policy slots_read on public.time_slots
  for select to anon, authenticated using (
    exists (select 1 from public.pitches p where p.id = pitch_id and p.is_active)
  );

drop policy if exists bookings_read_own on public.bookings;
create policy bookings_read_own on public.bookings
  for select to authenticated using ((select auth.uid()) = user_id);

create or replace function public.create_booking(
  p_user_id uuid, p_pitch_id text, p_slot_id text, p_booking_date date,
  p_customer_name text, p_customer_phone text, p_payment_method text
) returns setof public.bookings
language plpgsql security definer set search_path = '' as $$
declare
  s public.time_slots%rowtype;
  p public.pitches%rowtype;
  b public.bookings%rowtype;
  local_now timestamp := timezone('Asia/Ho_Chi_Minh', now());
begin
  if auth.uid() is null or auth.uid() <> p_user_id then
    raise exception 'permission_denied' using errcode = '42501';
  end if;
  if p_customer_name is null or length(trim(p_customer_name)) not between 1 and 120
     or p_customer_phone is null or length(trim(p_customer_phone)) not between 8 and 20
     or p_payment_method is null or p_payment_method not in ('cash', 'qr') then
    raise exception 'invalid_booking_details' using errcode = '22023';
  end if;
  if p_booking_date < local_now::date then raise exception 'booking_date_in_past'; end if;
  if p_booking_date > local_now::date + 7 then raise exception 'booking_date_too_far'; end if;
  select * into s from public.time_slots where id = p_slot_id
    and pitch_id = p_pitch_id and slot_date = p_booking_date for update;
  if not found then raise exception 'slot_not_found'; end if;
  if s.slot_date + s.starts_at <= local_now then raise exception 'slot_in_past'; end if;
  if s.status <> 'available' then raise exception 'slot_unavailable'; end if;
  select * into p from public.pitches where id = p_pitch_id and is_active;
  if not found then raise exception 'pitch_not_found'; end if;
  insert into public.bookings (
    booking_code, user_id, pitch_id, slot_id, booking_date, pitch_name,
    pitch_address, time_range, total_price, customer_name, customer_phone,
    payment_method, payment_status
  ) values (
    'SB-' || upper(encode(extensions.gen_random_bytes(6), 'hex')),
    auth.uid(), p_pitch_id, p_slot_id, p_booking_date, p.name, p.address,
    to_char(s.starts_at, 'HH24:MI') || ' - ' || to_char(s.ends_at, 'HH24:MI'),
    s.price, trim(p_customer_name), trim(p_customer_phone), p_payment_method,
    case when p_payment_method = 'cash' then 'cash_due' else 'qr_due' end
  ) returning * into b;
  update public.time_slots set status = 'booked' where id = s.id;
  return next b;
exception when unique_violation then raise exception 'slot_unavailable';
end;
$$;

create or replace function public.cancel_booking(p_user_id uuid, p_booking_id uuid)
returns setof public.bookings
language plpgsql security definer set search_path = '' as $$
declare b public.bookings%rowtype; s public.time_slots%rowtype;
begin
  if auth.uid() is null or auth.uid() <> p_user_id then
    raise exception 'permission_denied' using errcode = '42501';
  end if;
  select * into b from public.bookings where id = p_booking_id
    and user_id = auth.uid() for update;
  if not found then raise exception 'booking_not_found'; end if;
  if b.status = 'cancelled' then return next b; return; end if;
  select * into s from public.time_slots where id = b.slot_id for update;
  if s.slot_date + s.starts_at <= timezone('Asia/Ho_Chi_Minh', now()) then
    raise exception 'booking_not_cancellable';
  end if;
  update public.bookings set status = 'cancelled', cancelled_at = now()
    where id = b.id returning * into b;
  update public.time_slots set status = 'available' where id = s.id and status = 'booked';
  return next b;
end;
$$;

create or replace function public.reschedule_booking(
  p_user_id uuid, p_booking_id uuid, p_slot_id text, p_booking_date date
) returns setof public.bookings
language plpgsql security definer set search_path = '' as $$
declare
  b public.bookings%rowtype;
  old_s public.time_slots%rowtype;
  new_s public.time_slots%rowtype;
  local_now timestamp := timezone('Asia/Ho_Chi_Minh', now());
begin
  if auth.uid() is null or auth.uid() <> p_user_id then
    raise exception 'permission_denied' using errcode = '42501';
  end if;
  select * into b from public.bookings where id = p_booking_id
    and user_id = auth.uid() for update;
  if not found or b.status <> 'confirmed' then raise exception 'booking_not_found'; end if;
  if p_booking_date < local_now::date then raise exception 'booking_date_in_past'; end if;
  if p_booking_date > local_now::date + 7 then raise exception 'booking_date_too_far'; end if;
  perform id from public.time_slots where id in (b.slot_id, p_slot_id) order by id for update;
  select * into old_s from public.time_slots where id = b.slot_id;
  if old_s.slot_date + old_s.starts_at <= local_now then raise exception 'booking_not_cancellable'; end if;
  select * into new_s from public.time_slots where id = p_slot_id
    and pitch_id = b.pitch_id and slot_date = p_booking_date;
  if not found then raise exception 'slot_not_found'; end if;
  if new_s.id = old_s.id then return next b; return; end if;
  if new_s.slot_date + new_s.starts_at <= local_now then raise exception 'slot_in_past'; end if;
  if new_s.status <> 'available' then raise exception 'slot_unavailable'; end if;
  update public.time_slots set status = 'available' where id = old_s.id;
  update public.time_slots set status = 'booked' where id = new_s.id;
  update public.bookings set slot_id = new_s.id, booking_date = p_booking_date,
    time_range = to_char(new_s.starts_at, 'HH24:MI') || ' - ' || to_char(new_s.ends_at, 'HH24:MI'),
    total_price = new_s.price, rescheduled_at = now()
    where id = b.id returning * into b;
  return next b;
exception when unique_violation then raise exception 'slot_unavailable';
end;
$$;

revoke all on function public.create_booking(uuid, text, text, date, text, text, text)
  from public, anon, authenticated;
revoke all on function public.cancel_booking(uuid, uuid) from public, anon, authenticated;
revoke all on function public.reschedule_booking(uuid, uuid, text, date)
  from public, anon, authenticated;
grant execute on function public.create_booking(uuid, text, text, date, text, text, text)
  to authenticated;
grant execute on function public.cancel_booking(uuid, uuid) to authenticated;
grant execute on function public.reschedule_booking(uuid, uuid, text, date)
  to authenticated;

revoke insert, update, delete on public.bookings from anon, authenticated;
revoke insert, update, delete on public.time_slots from anon, authenticated;
