-- Remove only the seeded pitch fixtures and QA bookings created for app verification.
delete from public.bookings
where booking_code in ('SB-F89AF515', 'SB-712B48C6')
  and user_id in (
    select id from public.profiles
    where email = 'andzen.qa.20261014185704e8ec@example.com'
  );

delete from public.pitches
where (id, name) in (
  ('p1', 'Sân bóng Thượng Đình'),
  ('p2', 'Sân bóng Cầu Giấy Arena'),
  ('p3', 'Sân mini Mỹ Đình Stars'),
  ('p4', 'Sân vận động Trung Kính'),
  ('p5', 'Sân bóng Tân Triều Park'),
  ('p6', 'Sân bóng Keangnam Garden')
);

drop function if exists public.ensure_slots_for_date(text, date);

create or replace function public.create_booking(
    p_user_id uuid, p_pitch_id text, p_slot_id text, p_booking_date date,
    p_customer_name text, p_customer_phone text, p_payment_method text
)
returns setof public.bookings language plpgsql security definer set search_path = public as $$
declare
    selected_slot public.time_slots%rowtype;
    selected_pitch public.pitches%rowtype;
    new_booking public.bookings%rowtype;
    new_code text;
    business_today date := timezone('Asia/Ho_Chi_Minh', now())::date;
begin
    if p_booking_date < business_today then raise exception 'booking_date_in_past'; end if;
    if p_booking_date > business_today + 7 then raise exception 'booking_date_too_far'; end if;

    select * into selected_slot from public.time_slots
      where id = p_slot_id and pitch_id = p_pitch_id and slot_date = p_booking_date for update;
    if not found then raise exception 'slot_not_found'; end if;
    if selected_slot.status <> 'available' then raise exception 'slot_unavailable'; end if;
    select * into selected_pitch from public.pitches where id = p_pitch_id and is_active;
    if not found then raise exception 'pitch_not_found'; end if;
    new_code := 'SB-' || upper(substr(encode(extensions.gen_random_bytes(4), 'hex'), 1, 8));

    insert into public.bookings (
      booking_code, user_id, pitch_id, slot_id, booking_date, pitch_name, pitch_address,
      time_range, total_price, customer_name, customer_phone, payment_method, payment_status
    ) values (
      new_code, p_user_id, p_pitch_id, p_slot_id, p_booking_date, selected_pitch.name,
      selected_pitch.address,
      to_char(selected_slot.starts_at, 'HH24:MI') || ' - ' || to_char(selected_slot.ends_at, 'HH24:MI'),
      selected_slot.price, p_customer_name, p_customer_phone, p_payment_method,
      case when p_payment_method = 'cash' then 'cash_due' else 'qr_due' end
    ) returning * into new_booking;

    update public.time_slots set status = 'booked' where id = p_slot_id;
    return next new_booking;
exception when unique_violation then
    raise exception 'slot_unavailable';
end;
$$;

create or replace function public.reschedule_booking(
    p_user_id uuid, p_booking_id uuid, p_slot_id text, p_booking_date date
)
returns setof public.bookings language plpgsql security definer set search_path = public as $$
declare
    selected_booking public.bookings%rowtype;
    old_slot public.time_slots%rowtype;
    new_slot public.time_slots%rowtype;
    business_today date := timezone('Asia/Ho_Chi_Minh', now())::date;
begin
    select * into selected_booking from public.bookings
      where id = p_booking_id and user_id = p_user_id for update;
    if not found or selected_booking.status <> 'confirmed' then raise exception 'booking_not_found'; end if;
    if p_booking_date < business_today then raise exception 'booking_date_in_past'; end if;
    if p_booking_date > business_today + 7 then raise exception 'booking_date_too_far'; end if;

    select * into old_slot from public.time_slots where id = selected_booking.slot_id for update;
    select * into new_slot from public.time_slots
      where id = p_slot_id and pitch_id = selected_booking.pitch_id and slot_date = p_booking_date for update;
    if not found or new_slot.status <> 'available' then raise exception 'slot_unavailable'; end if;
    if new_slot.id = old_slot.id then return next selected_booking; return; end if;

    update public.time_slots set status = 'available'
      where id = old_slot.id
        and not exists (
          select 1 from public.bookings
          where slot_id = old_slot.id and status = 'confirmed' and id <> selected_booking.id
        );
    update public.time_slots set status = 'booked' where id = new_slot.id;
    update public.bookings set slot_id = new_slot.id, booking_date = p_booking_date,
      time_range = to_char(new_slot.starts_at, 'HH24:MI') || ' - ' || to_char(new_slot.ends_at, 'HH24:MI'),
      total_price = new_slot.price, rescheduled_at = now()
      where id = selected_booking.id returning * into selected_booking;
    return next selected_booking;
end;
$$;

revoke all on function public.reschedule_booking(uuid, uuid, text, date) from public, anon, authenticated;
grant execute on function public.reschedule_booking(uuid, uuid, text, date) to service_role;
