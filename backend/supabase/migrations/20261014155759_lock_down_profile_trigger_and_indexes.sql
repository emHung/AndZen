revoke all on function public.create_profile_for_auth_user() from public, anon, authenticated;

create index if not exists bookings_by_pitch_id on public.bookings(pitch_id);
create index if not exists favorites_by_pitch_id on public.favorites(pitch_id);
