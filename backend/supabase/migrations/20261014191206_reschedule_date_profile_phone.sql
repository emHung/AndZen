alter table public.bookings
  add column if not exists rescheduled_at timestamptz;

grant update (full_name, email, phone, avatar_path, avatar_url, gender, birthdate, district, updated_at)
  on public.profiles to authenticated;

drop function if exists public.reschedule_booking(uuid, uuid, text);

create or replace function public.reschedule_booking(
    p_user_id uuid,
    p_booking_id uuid,
    p_slot_id text,
    p_booking_date date
)
returns setof public.bookings
language plpgsql
security definer
set search_path = public
as $$
declare
    selected_booking public.bookings%rowtype;
    old_slot public.time_slots%rowtype;
    new_slot public.time_slots%rowtype;
begin
    select * into selected_booking
      from public.bookings
      where id = p_booking_id and user_id = p_user_id
      for update;
    if not found or selected_booking.status <> 'confirmed' then
      raise exception 'booking_not_found';
    end if;
    if p_booking_date < current_date then
      raise exception 'booking_date_in_past';
    end if;

    select * into old_slot from public.time_slots where id = selected_booking.slot_id for update;
    select * into new_slot
      from public.time_slots
      where id = p_slot_id
        and pitch_id = selected_booking.pitch_id
        and slot_date = p_booking_date
      for update;
    if not found or new_slot.status <> 'available' then
      raise exception 'slot_unavailable';
    end if;
    if new_slot.id = old_slot.id then
      return next selected_booking;
      return;
    end if;

    update public.time_slots
      set status = 'available'
      where id = old_slot.id
        and not exists (
          select 1 from public.bookings
          where slot_id = old_slot.id and status = 'confirmed' and id <> selected_booking.id
        );
    update public.time_slots set status = 'booked' where id = new_slot.id;
    update public.bookings
      set slot_id = new_slot.id,
          booking_date = p_booking_date,
          time_range = to_char(new_slot.starts_at, 'HH24:MI') || ' - ' || to_char(new_slot.ends_at, 'HH24:MI'),
          total_price = new_slot.price,
          rescheduled_at = now()
      where id = selected_booking.id
      returning * into selected_booking;
    return next selected_booking;
end;
$$;

revoke all on function public.reschedule_booking(uuid, uuid, text, date) from public, anon, authenticated;
grant execute on function public.reschedule_booking(uuid, uuid, text, date) to service_role;
