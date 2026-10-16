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
begin
    select * into selected_slot from public.time_slots
      where id = p_slot_id and pitch_id = p_pitch_id and slot_date = p_booking_date for update;
    if not found then raise exception 'slot_not_found'; end if;
    if p_booking_date < current_date then raise exception 'booking_date_in_past'; end if;
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
