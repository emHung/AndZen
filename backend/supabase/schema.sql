-- ==============================================================================
-- AndZen Database Schema — Toàn bộ cấu trúc CSDL khởi tạo (All-in-One Schema)
-- Phù hợp để chạy 1 lần trực tiếp trong Supabase SQL Editor
-- ==============================================================================

-- 1. Bật các extension cần thiết
create extension if not exists pgcrypto;
create extension if not exists btree_gist with schema extensions;

-- 2. Bảng thông tin người dùng mở rộng (Profiles)
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

-- 3. Bảng thiết lập người dùng (User Settings)
create table if not exists public.user_settings (
    user_id uuid primary key references auth.users(id) on delete cascade,
    notifications_enabled boolean not null default true,
    profile_public boolean not null default false,
    updated_at timestamptz not null default now()
);

-- 4. Bảng danh mục sân bóng (Pitches)
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

-- 5. Bảng khung giờ chơi (Time Slots)
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

-- Chống trùng lặp giờ chồng lấn (No overlapping slots for same pitch and date)
alter table public.time_slots drop constraint if exists no_overlapping_pitch_slots;
alter table public.time_slots add constraint no_overlapping_pitch_slots
exclude using gist (
  pitch_id with =,
  tsrange(slot_date + starts_at, slot_date + ends_at, '[)') with &&
);

-- 6. Bảng đơn đặt sân (Bookings)
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
    cancelled_at timestamptz,
    rescheduled_at timestamptz
);

-- Ràng buộc: Mỗi slot chỉ có tối đa 1 đơn confirmed (Chống Double-Booking ở tầng DB)
create unique index if not exists one_confirmed_booking_per_slot
    on public.bookings(slot_id) where status = 'confirmed';
create index if not exists bookings_by_user_date on public.bookings(user_id, booking_date desc);

-- 7. Bảng sân yêu thích (Favorites)
create table if not exists public.favorites (
    user_id uuid not null references auth.users(id) on delete cascade,
    pitch_id text not null references public.pitches(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, pitch_id)
);

-- 8. Bảng phân quyền nhân viên quản lý sân (Staff Pitch Assignments)
create table if not exists public.staff_pitch_assignments (
  user_id uuid not null references public.profiles(id) on delete cascade,
  pitch_id text not null references public.pitches(id) on delete cascade,
  primary key (user_id, pitch_id)
);
create index if not exists staff_assignments_by_pitch on public.staff_pitch_assignments(pitch_id);

-- ==============================================================================
-- CÁC HÀM VÀ THỦ TỤC NGHIỆP VỤ (Functions & Procedures)
-- ==============================================================================

-- Trigger tạo Profile tự động khi người dùng đăng ký qua Supabase Auth
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

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
    after insert on auth.users
    for each row execute procedure public.create_profile_for_auth_user();

-- Hàm kiểm tra quyền quản lý sân (Admin hoặc Staff được phân công)
create or replace function public.can_manage_pitch(p_user_id uuid, p_pitch_id text)
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

-- Hàm phân quyền nhân viên cho sân
create or replace function public.assign_staff_pitches(p_actor_id uuid, p_staff_id uuid, p_pitch_ids text[])
returns void language plpgsql security definer set search_path = '' as $$
begin
 if not exists(select 1 from public.profiles where id = p_actor_id and role = 'admin') then
   raise exception 'permission_denied';
 end if;
 perform id from public.profiles where id = p_staff_id and role = 'staff' for update;
 if not found then raise exception 'staff_not_found'; end if;
 if exists(select 1 from unnest(p_pitch_ids) x where not exists(select 1 from public.pitches p where p.id = x)) then
   raise exception 'pitch_not_found';
 end if;
 delete from public.staff_pitch_assignments where user_id = p_staff_id;
 insert into public.staff_pitch_assignments(user_id, pitch_id)
   select p_staff_id, x from unnest(p_pitch_ids) x on conflict do nothing;
end;
$$;
revoke all on function public.assign_staff_pitches(uuid, uuid, text[]) from public, anon, authenticated;
grant execute on function public.assign_staff_pitches(uuid, uuid, text[]) to service_role;

-- Hàm tạo đơn đặt sân (Atomic Booking with Concurrency Lock)
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
 select * into s from public.time_slots where id = p_slot_id and pitch_id = p_pitch_id
   and slot_date = p_booking_date for update;
 if not found then raise exception 'slot_not_found'; end if;
 if s.slot_date + s.starts_at <= local_now then raise exception 'slot_in_past'; end if;
 if s.status <> 'available' then raise exception 'slot_unavailable'; end if;
 select * into p from public.pitches where id = p_pitch_id and is_active;
 if not found then raise exception 'pitch_not_found'; end if;
 insert into public.bookings(
   booking_code, user_id, pitch_id, slot_id, booking_date, pitch_name, pitch_address,
   time_range, total_price, customer_name, customer_phone, payment_method, payment_status
 ) values (
   'SB-' || upper(encode(extensions.gen_random_bytes(4), 'hex')),
   p_user_id, p_pitch_id, p_slot_id, p_booking_date, p.name, p.address,
   to_char(s.starts_at, 'HH24:MI') || ' - ' || to_char(s.ends_at, 'HH24:MI'),
   s.price, p_customer_name, p_customer_phone, p_payment_method,
   case when p_payment_method = 'cash' then 'cash_due' else 'qr_due' end
 ) returning * into b;
 update public.time_slots set status = 'booked' where id = s.id;
 return next b;
exception when unique_violation then
 raise exception 'slot_unavailable';
end;
$$;
revoke all on function public.create_booking(uuid, text, text, date, text, text, text) from public, anon, authenticated;
grant execute on function public.create_booking(uuid, text, text, date, text, text, text) to service_role;

-- Hàm hủy đặt sân (Cancel Booking & Free Slot)
create or replace function public.cancel_booking(p_user_id uuid, p_booking_id uuid)
returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare
 b public.bookings%rowtype; s public.time_slots%rowtype;
 local_now timestamp := timezone('Asia/Ho_Chi_Minh', now());
begin
 select * into b from public.bookings where id = p_booking_id and user_id = p_user_id for update;
 if not found then raise exception 'booking_not_found'; end if;
 if b.status <> 'confirmed' then raise exception 'booking_not_cancellable'; end if;
 select * into s from public.time_slots where id = b.slot_id for update;
 if s.slot_date + s.starts_at <= local_now then raise exception 'booking_in_past'; end if;
 update public.bookings set status = 'cancelled', cancelled_at = now() where id = b.id returning * into b;
 update public.time_slots set status = 'available' where id = s.id;
 return next b;
end;
$$;
revoke all on function public.cancel_booking(uuid, uuid) from public, anon, authenticated;
grant execute on function public.cancel_booking(uuid, uuid) to service_role;

-- Hàm đổi lịch đặt sân (Reschedule Booking)
create or replace function public.reschedule_booking(
    p_user_id uuid, p_booking_id uuid, p_slot_id text, p_booking_date date
) returns setof public.bookings language plpgsql security definer set search_path = '' as $$
declare
    selected_booking public.bookings%rowtype;
    old_slot public.time_slots%rowtype;
    new_slot public.time_slots%rowtype;
    local_now timestamp := timezone('Asia/Ho_Chi_Minh', now());
begin
    select * into selected_booking from public.bookings
      where id = p_booking_id and user_id = p_user_id for update;
    if not found or selected_booking.status <> 'confirmed' then raise exception 'booking_not_found'; end if;
    if p_booking_date < local_now::date then raise exception 'booking_date_in_past'; end if;
    if p_booking_date > local_now::date + 7 then raise exception 'booking_date_too_far'; end if;

    select * into old_slot from public.time_slots where id = selected_booking.slot_id for update;
    select * into new_slot from public.time_slots
      where id = p_slot_id and pitch_id = selected_booking.pitch_id and slot_date = p_booking_date for update;
    if not found or new_slot.status <> 'available' then raise exception 'slot_unavailable'; end if;
    if new_slot.slot_date + new_slot.starts_at <= local_now then raise exception 'slot_in_past'; end if;
    if new_slot.id = old_slot.id then return next selected_booking; return; end if;

    update public.time_slots set status = 'available' where id = old_slot.id;
    update public.time_slots set status = 'booked' where id = new_slot.id;
    update public.bookings set
      slot_id = new_slot.id, booking_date = p_booking_date,
      time_range = to_char(new_slot.starts_at, 'HH24:MI') || ' - ' || to_char(new_slot.ends_at, 'HH24:MI'),
      total_price = new_slot.price, rescheduled_at = now()
    where id = selected_booking.id returning * into selected_booking;
    return next selected_booking;
end;
$$;
revoke all on function public.reschedule_booking(uuid, uuid, text, date) from public, anon, authenticated;
grant execute on function public.reschedule_booking(uuid, uuid, text, date) to service_role;

-- ==============================================================================
-- BẢO MẬT DỮ LIỆU ROW LEVEL SECURITY (RLS)
-- ==============================================================================
alter table public.profiles enable row level security;
alter table public.pitches enable row level security;
alter table public.time_slots enable row level security;
alter table public.bookings enable row level security;
alter table public.favorites enable row level security;
alter table public.user_settings enable row level security;

-- Profiles: Người dùng xem và sửa hồ sơ của chính mình
drop policy if exists "Profiles are viewable by owner or public" on public.profiles;
create policy "Profiles are viewable by owner or public" on public.profiles
    for select using (auth.uid() = id or exists (select 1 from public.user_settings s where s.user_id = id and s.profile_public));

drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile" on public.profiles
    for update using (auth.uid() = id);

-- Pitches & Slots: Công khai cho mọi người đọc
drop policy if exists "Pitches are viewable by everyone" on public.pitches;
create policy "Pitches are viewable by everyone" on public.pitches
    for select using (is_active = true);

drop policy if exists "Time slots are viewable by everyone" on public.time_slots;
create policy "Time slots are viewable by everyone" on public.time_slots
    for select using (true);

-- Bookings: Chỉ xem đơn của chính mình
drop policy if exists "Users can view own bookings" on public.bookings;
create policy "Users can view own bookings" on public.bookings
    for select using (auth.uid() = user_id);

-- Favorites: Quản lý danh sách yêu thích của chính mình
drop policy if exists "Users can manage favorites" on public.favorites;
create policy "Users can manage favorites" on public.favorites
    for all using (auth.uid() = user_id);

-- User Settings: Quản lý thiết lập cá nhân
drop policy if exists "Users manage own settings" on public.user_settings;
create policy "Users manage own settings" on public.user_settings
    for all using (auth.uid() = user_id);

-- Cấp toàn quyền cho backend service_role
grant all on all tables in schema public to service_role;
grant all on all sequences in schema public to service_role;
grant all on all routines in schema public to service_role;
