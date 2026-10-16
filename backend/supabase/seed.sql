-- ==============================================================================
-- AndZen Seed Data — Dữ liệu mẫu khởi tạo (Sample Pitches & Dynamic Time Slots)
-- Chạy file này trong Supabase SQL Editor sau khi đã chạy schema.sql
-- ==============================================================================

-- 1. Nạp danh mục 6 sân bóng thực tế tại Hà Nội
insert into public.pitches (id, name, district, address, pitch_type, base_price, rating, amenities, description, image_key, is_active)
values
  (
    'p1',
    'Sân bóng Thượng Đình',
    'Thanh Xuân',
    '129 Nguyễn Trãi, Thanh Xuân, Hà Nội',
    'SEVEN',
    350000,
    4.8,
    array['Sân 7 người', 'Đèn LED', 'Nước miễn phí', 'Bãi xe rộng', 'Áo bib'],
    'Mặt cỏ nhân tạo cao cấp mới thay, hệ thống dàn đèn LED tiêu chuẩn thi đấu ban đêm, thoát nước tốt và có bảo vệ trông xe an toàn.',
    'img_pitch_1',
    true
  ),
  (
    'p2',
    'Sân bóng Cầu Giấy Arena',
    'Cầu Giấy',
    '35 Dịch Vọng Hậu, Cầu Giấy, Hà Nội',
    'SEVEN',
    400000,
    4.7,
    array['Sân 7 người', 'Phòng thay đồ', 'Bãi xe ô tô', 'Căn tin', 'Đèn LED'],
    'Khu phức hợp thể thao hiện đại bậc nhất Cầu Giấy, có khán đài mini, phòng tắm nóng lạnh và quầy nước giải khát đầy đủ.',
    'img_pitch_2',
    true
  ),
  (
    'p3',
    'Sân mini Mỹ Đình Stars',
    'Nam Từ Liêm',
    'Lê Đức Thọ, Mỹ Đình, Nam Từ Liêm, Hà Nội',
    'FIVE',
    250000,
    4.6,
    array['Sân 5 người', 'Đèn cao áp', 'Gửi xe máy ô tô', 'Áo bib', 'Nước uống'],
    'Chuyên cụm sân 5 người chất lượng cao, cỏ êm chân hạn chế chấn thương, phù hợp đá giao lưu nội bộ văn phòng sau giờ làm.',
    'img_pitch_sunset',
    true
  ),
  (
    'p4',
    'Sân vận động Trung Kính',
    'Cầu Giấy',
    '215 Trung Kính, Cầu Giấy, Hà Nội',
    'FIVE',
    280000,
    4.5,
    array['Sân 5 người', 'Đèn LED', 'Tủ đồ cá nhân', 'Căn tin'],
    'Vị trí trung tâm thuận tiện di chuyển giữa các quận, sân cỏ được vệ sinh và bảo dưỡng định kỳ hàng tuần.',
    'img_pitch_night',
    true
  ),
  (
    'p5',
    'Sân bóng Tân Triều Park',
    'Thanh Xuân',
    'Ngõ 300 Nguyễn Xiển, Thanh Xuân, Hà Nội',
    'ELEVEN',
    900000,
    4.9,
    array['Sân 11 người', 'Khán đài', 'Phòng tắm nóng lạnh', 'Bãi xe ô tô', 'Trọng tài'],
    'Sân bóng 11 người đạt tiêu chuẩn tổ chức các giải đấu phong trào lớn, kích thước tiêu chuẩn FIFA, mặt sân phẳng và cỏ mềm.',
    'img_pitch_1',
    true
  ),
  (
    'p6',
    'Sân bóng Keangnam Garden',
    'Nam Từ Liêm',
    'Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội',
    'SEVEN',
    420000,
    4.8,
    array['Sân 7 người', 'Đèn LED', 'Cho thuê giày', 'Wifi miễn phí', 'Bãi xe'],
    'Không gian thoáng mát cạnh tháp Keangnam Landmark, hệ thống lưới chắn an toàn, dịch vụ chuyên nghiệp và thân thiện.',
    'img_pitch_2',
    true
  )
on conflict (id) do update set
  name = excluded.name,
  district = excluded.district,
  address = excluded.address,
  pitch_type = excluded.pitch_type,
  base_price = excluded.base_price,
  rating = excluded.rating,
  amenities = excluded.amenities,
  description = excluded.description,
  image_key = excluded.image_key,
  is_active = excluded.is_active;

-- 2. Tự động sinh Khung Giờ (Time Slots) động cho 7 ngày tới (Hôm nay -> Hôm nay + 7 ngày)
-- Chạy đoạn mã này đảm bảo bất cứ khi nào import, các khung giờ luôn luôn sẵn sàng cho tuần hiện tại
do $$
declare
  pitch_record record;
  target_date date;
  today_date date := timezone('Asia/Ho_Chi_Minh', now())::date;
  day_offset int;
  slot_record record;
  slot_id_val text;
  slot_price bigint;
  ranges text[] := array[
    '06:00-07:30',
    '07:30-09:00',
    '09:00-10:30',
    '10:30-12:00',
    '12:00-13:30',
    '13:30-15:00',
    '15:00-16:30',
    '16:30-18:00',
    '18:00-19:30',
    '19:30-21:00',
    '21:00-22:30'
  ];
  range_text text;
  start_str text;
  end_str text;
  range_idx int;
begin
  for pitch_record in select id, base_price from public.pitches where is_active loop
    for day_offset in 0..7 loop
      target_date := today_date + day_offset;
      range_idx := 0;
      
      foreach range_text in array ranges loop
        range_idx := range_idx + 1;
        start_str := split_part(range_text, '-', 1);
        end_str := split_part(range_text, '-', 2);
        
        -- Khung giờ tối (sau 16:30) tăng phụ phí 50.000đ so với giá gốc
        if start_str >= '16:30' and start_str <= '21:00' then
          slot_price := pitch_record.base_price + 50000;
        else
          slot_price := pitch_record.base_price;
        end if;
        
        slot_id_val := pitch_record.id || '_' || to_char(target_date, 'YYYYMMDD') || '_' || lpad(range_idx::text, 2, '0');
        
        insert into public.time_slots (
          id, pitch_id, slot_date, starts_at, ends_at, price, status
        ) values (
          slot_id_val,
          pitch_record.id,
          target_date,
          start_str::time,
          end_str::time,
          slot_price,
          'available'
        )
        on conflict (pitch_id, slot_date, starts_at) do update set
          price = excluded.price;
      end loop;
    end loop;
  end loop;
end;
$$;

-- 3. Thông báo hoàn tất
select
  count(*) as total_pitches,
  (select count(*) from public.time_slots) as total_generated_slots
from public.pitches;
