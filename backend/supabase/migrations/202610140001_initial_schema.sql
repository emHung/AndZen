create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text not null default '',
    email text,
    phone text,
    avatar_path text,
    avatar_url text,
    gender text check (gender is null or gender in ('Nam', 'Nữ', 'Khác')),
    birthdate date,
    district text,
    role text not null default 'customer' check (role in ('customer', 'staff', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create or replace function public.create_profile_for_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    insert into public.profiles (id, full_name, email, phone)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'full_name', ''),
        new.email,
        coalesce(new.phone, new.raw_user_meta_data ->> 'phone')
    ) on conflict (id) do nothing;
    insert into public.user_settings (user_id) values (new.id) on conflict (user_id) do nothing;
    return new;
end;
$$;

create table if not exists public.pitches (
    id text primary key,
    name text not null,
    district text not null,
    address text not null,
    pitch_type text not null check (pitch_type in ('FIVE', 'SEVEN', 'ELEVEN')),
    base_price bigint not null check (base_price > 0),
    rating numeric(2,1) not null default 0 check (rating between 0 and 5),
    amenities text[] not null default '{}',
    description text not null default '',
    image_key text,
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists public.time_slots (
    id text primary key,
    pitch_id text not null references public.pitches(id) on delete cascade,
    slot_date date not null,
    starts_at time not null,
    ends_at time not null,
    price bigint not null check (price > 0),
    status text not null default 'available' check (status in ('available', 'booked', 'unavailable')),
    unique (pitch_id, slot_date, starts_at),
    check (ends_at > starts_at)
);

create table if not exists public.bookings (
    id uuid primary key default gen_random_uuid(),
    booking_code text not null unique,
    user_id uuid not null references auth.users(id) on delete cascade,
    pitch_id text not null references public.pitches(id),
    slot_id text not null references public.time_slots(id),
    booking_date date not null,
    pitch_name text not null,
    pitch_address text not null,
    time_range text not null,
    total_price bigint not null check (total_price > 0),
    customer_name text not null,
    customer_phone text not null,
    payment_method text not null check (payment_method in ('cash', 'qr')),
    payment_status text not null check (payment_status in ('cash_due', 'qr_due', 'paid')),
    status text not null default 'confirmed' check (status in ('confirmed', 'cancelled')),
    created_at timestamptz not null default now(),
    cancelled_at timestamptz
);

create unique index if not exists one_confirmed_booking_per_slot
    on public.bookings(slot_id) where status = 'confirmed';
create index if not exists bookings_by_user_date on public.bookings(user_id, booking_date desc);

create table if not exists public.favorites (
    user_id uuid not null references auth.users(id) on delete cascade,
    pitch_id text not null references public.pitches(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, pitch_id)
);

create table if not exists public.user_settings (
    user_id uuid primary key references auth.users(id) on delete cascade,
    notifications_enabled boolean not null default true,
    profile_public boolean not null default false,
    updated_at timestamptz not null default now()
);

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
    after insert on auth.users
    for each row execute procedure public.create_profile_for_auth_user();

insert into public.pitches (id, name, district, address, pitch_type, base_price, rating, amenities, description, image_key)
values
 ('p1', 'Sân bóng Thượng Đình', 'Thanh Xuân', '129 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'SEVEN', 350000, 4.8, array['Sân 7 người','Đèn LED','Nước miễn phí','Bãi xe rộng'], 'Mặt cỏ nhân tạo mới thay năm 2024, hệ thống thoát nước tốt, đèn sáng chuẩn thi đấu.', 'img_pitch_1'),
 ('p2', 'Sân bóng Cầu Giấy Arena', 'Cầu Giấy', '35 Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 'SEVEN', 400000, 4.7, array['Sân 7 người','Có mái che','Phòng thay đồ','Bảo vệ 24/7'], 'Khu phức hợp thể thao hiện đại, phục vụ căng tin và nước giải khát đầy đủ.', 'img_pitch_2'),
 ('p3', 'Sân mini Mỹ Đình Stars', 'Nam Từ Liêm', 'Lê Đức Thọ, Mỹ Đình, Nam Từ Liêm, Hà Nội', 'FIVE', 250000, 4.6, array['Sân 5 người','Đèn cao áp','Gửi xe máy ô tô','Áo pitch tập'], 'Chuyên sân 5 người chất lượng cao, cỏ êm chân, phù hợp đá giao lưu sau giờ làm.', 'img_pitch_sunset'),
 ('p4', 'Sân vận động Trung Kính', 'Cầu Giấy', '215 Trung Kính, Cầu Giấy, Hà Nội', 'FIVE', 280000, 4.5, array['Sân 5 người','Đèn LED','Tủ đồ cá nhân','Căng tin'], 'Vị trí trung tâm thuận tiện di chuyển, sân cỏ được bảo dưỡng định kỳ hàng tháng.', 'img_pitch_night'),
 ('p5', 'Sân bóng Tân Triều Park', 'Thanh Xuân', 'Ngõ 300 Nguyễn Xiển, Thanh Xuân, Hà Nội', 'ELEVEN', 900000, 4.9, array['Sân 11 người','Khán đài','Phòng tắm nóng lạnh','Bãi xe'], 'Sân bóng 11 người đạt tiêu chuẩn giải đấu phong trào, mặt sân rộng rãi bằng phẳng.', 'img_pitch_1'),
 ('p6', 'Sân bóng Keangnam Garden', 'Nam Từ Liêm', 'Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội', 'SEVEN', 420000, 4.8, array['Sân 7 người','Đèn LED','Cho thuê giày','Wifi miễn phí'], 'Không gian thoáng mát cạnh tòa Keangnam, hệ thống lưới chắn và cỏ mềm an toàn.', 'img_pitch_2')
on conflict (id) do update set
 name = excluded.name, district = excluded.district, address = excluded.address,
 pitch_type = excluded.pitch_type, base_price = excluded.base_price,
 rating = excluded.rating, amenities = excluded.amenities,
 description = excluded.description, image_key = excluded.image_key;

create or replace function public.ensure_slots_for_date(p_pitch_id text, p_date date)
returns void language plpgsql security definer set search_path = public as $$
declare
    base_price_value bigint;
begin
    select base_price into base_price_value from public.pitches where id = p_pitch_id and is_active;
    if base_price_value is null then raise exception 'pitch_not_found'; end if;

    insert into public.time_slots (id, pitch_id, slot_date, starts_at, ends_at, price)
    select
        p_pitch_id || '_' || p_date::text || '_' || slot.idx::text,
        p_pitch_id, p_date, slot.start_at::time, slot.end_at::time,
        base_price_value + case when slot.idx >= 4 then 50000 else 0 end
    from (values
        (0, '06:00', '07:30'), (1, '07:30', '09:00'), (2, '15:00', '16:30'),
        (3, '16:30', '18:00'), (4, '18:00', '19:30'), (5, '19:30', '21:00'),
        (6, '21:00', '22:30')
    ) as slot(idx, start_at, end_at)
    on conflict (pitch_id, slot_date, starts_at) do nothing;
end;
$$;

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

create or replace function public.cancel_booking(p_user_id uuid, p_booking_id uuid)
returns setof public.bookings language plpgsql security definer set search_path = public as $$
declare
    selected_booking public.bookings%rowtype;
begin
    select * into selected_booking from public.bookings where id = p_booking_id and user_id = p_user_id for update;
    if not found then raise exception 'booking_not_found'; end if;
    if selected_booking.status <> 'confirmed' or selected_booking.booking_date < current_date then
      raise exception 'booking_not_cancellable';
    end if;
    update public.bookings set status = 'cancelled', cancelled_at = now() where id = p_booking_id returning * into selected_booking;
    if not exists (select 1 from public.bookings where slot_id = selected_booking.slot_id and status = 'confirmed') then
      update public.time_slots set status = 'available' where id = selected_booking.slot_id and status = 'booked';
    end if;
    return next selected_booking;
end;
$$;

create or replace function public.reschedule_booking(p_user_id uuid, p_booking_id uuid, p_slot_id text)
returns setof public.bookings language plpgsql security definer set search_path = public as $$
declare
    selected_booking public.bookings%rowtype;
    old_slot public.time_slots%rowtype;
    new_slot public.time_slots%rowtype;
begin
    select * into selected_booking from public.bookings where id = p_booking_id and user_id = p_user_id for update;
    if not found or selected_booking.status <> 'confirmed' then raise exception 'booking_not_found'; end if;
    select * into old_slot from public.time_slots where id = selected_booking.slot_id for update;
    if p_slot_id = selected_booking.slot_id then return next selected_booking; return; end if;
    select * into new_slot from public.time_slots
      where id = p_slot_id and pitch_id = selected_booking.pitch_id and slot_date = selected_booking.booking_date for update;
    if not found or new_slot.status <> 'available' then raise exception 'slot_unavailable'; end if;

    update public.time_slots set status = 'available' where id = old_slot.id and old_slot.id <> new_slot.id;
    update public.time_slots set status = 'booked' where id = new_slot.id;
    update public.bookings set slot_id = new_slot.id,
      time_range = to_char(new_slot.starts_at, 'HH24:MI') || ' - ' || to_char(new_slot.ends_at, 'HH24:MI'),
      total_price = new_slot.price
      where id = selected_booking.id returning * into selected_booking;
    return next selected_booking;
end;
$$;

alter table public.profiles enable row level security;
alter table public.pitches enable row level security;
alter table public.time_slots enable row level security;
alter table public.bookings enable row level security;
alter table public.favorites enable row level security;
alter table public.user_settings enable row level security;

revoke all on public.profiles, public.pitches, public.time_slots, public.bookings, public.favorites, public.user_settings from anon, authenticated;
grant select on public.pitches, public.time_slots to authenticated;
grant select on public.profiles to authenticated;
grant update (full_name, email, avatar_path, avatar_url, gender, birthdate, district) on public.profiles to authenticated;
grant select, insert, delete on public.favorites to authenticated;
grant select, insert, update on public.user_settings to authenticated;

create policy "profiles_read_own" on public.profiles for select to authenticated using (id = (select auth.uid()));
create policy "profiles_update_own" on public.profiles for update to authenticated using (id = (select auth.uid())) with check (id = (select auth.uid()));
create policy "pitches_read_active" on public.pitches for select to authenticated using (is_active);
create policy "slots_read" on public.time_slots for select to authenticated using (true);
create policy "favorites_read_own" on public.favorites for select to authenticated using (user_id = (select auth.uid()));
create policy "favorites_insert_own" on public.favorites for insert to authenticated with check (user_id = (select auth.uid()));
create policy "favorites_delete_own" on public.favorites for delete to authenticated using (user_id = (select auth.uid()));
create policy "settings_read_own" on public.user_settings for select to authenticated using (user_id = (select auth.uid()));
create policy "settings_insert_own" on public.user_settings for insert to authenticated with check (user_id = (select auth.uid()));
create policy "settings_update_own" on public.user_settings for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

revoke all on function public.ensure_slots_for_date(text, date) from public, anon, authenticated;
revoke all on function public.create_booking(uuid, text, text, date, text, text, text) from public, anon, authenticated;
revoke all on function public.cancel_booking(uuid, uuid) from public, anon, authenticated;
revoke all on function public.reschedule_booking(uuid, uuid, text) from public, anon, authenticated;
grant execute on function public.ensure_slots_for_date(text, date) to service_role;
grant execute on function public.create_booking(uuid, text, text, date, text, text, text) to service_role;
grant execute on function public.cancel_booking(uuid, uuid) to service_role;
grant execute on function public.reschedule_booking(uuid, uuid, text) to service_role;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 5242880, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public = true, file_size_limit = 5242880, allowed_mime_types = excluded.allowed_mime_types;

create policy "avatar_read_public" on storage.objects for select to public using (bucket_id = 'avatars');
create policy "avatar_insert_own_folder" on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));
create policy "avatar_update_own_folder" on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid()::text))
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));
create policy "avatar_delete_own_folder" on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid()::text));

