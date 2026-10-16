#!/usr/bin/env python3
"""Script nạp dữ liệu mẫu (Seed Data) cho Supabase Database của AndZen.

Cách chạy:
    python backend/scripts/seed_supabase.py
    hoặc:
    python backend/scripts/seed_supabase.py --apply
"""

from datetime import datetime, timedelta
import os
from pathlib import Path
import sys
from zoneinfo import ZoneInfo

from dotenv import load_dotenv
from supabase import create_client

sys.stdout.reconfigure(encoding="utf-8")
ROOT = Path(__file__).resolve().parent.parent
load_dotenv(ROOT / ".env")

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY") or os.getenv("SUPABASE_ANON_KEY")

if not SUPABASE_URL or not SUPABASE_KEY:
    print("❌ Lỗi: Chưa cấu hình SUPABASE_URL hoặc SUPABASE_SERVICE_ROLE_KEY trong file backend/.env")
    sys.exit(1)

supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

PITCHES = [
    {
        "id": "p1",
        "name": "Sân bóng Thượng Đình",
        "district": "Thanh Xuân",
        "address": "129 Nguyễn Trãi, Thanh Xuân, Hà Nội",
        "pitch_type": "SEVEN",
        "base_price": 350000,
        "rating": 4.8,
        "amenities": ["Sân 7 người", "Đèn LED", "Nước miễn phí", "Bãi xe rộng", "Áo bib"],
        "description": "Mặt cỏ nhân tạo cao cấp mới thay, hệ thống dàn đèn LED tiêu chuẩn thi đấu ban đêm, thoát nước tốt và có bảo vệ trông xe an toàn.",
        "image_key": "img_pitch_1",
        "is_active": True,
    },
    {
        "id": "p2",
        "name": "Sân bóng Cầu Giấy Arena",
        "district": "Cầu Giấy",
        "address": "35 Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
        "pitch_type": "SEVEN",
        "base_price": 400000,
        "rating": 4.7,
        "amenities": ["Sân 7 người", "Phòng thay đồ", "Bãi xe ô tô", "Căn tin", "Đèn LED"],
        "description": "Khu phức hợp thể thao hiện đại bậc nhất Cầu Giấy, có khán đài mini, phòng tắm nóng lạnh và quầy nước giải khát đầy đủ.",
        "image_key": "img_pitch_2",
        "is_active": True,
    },
    {
        "id": "p3",
        "name": "Sân mini Mỹ Đình Stars",
        "district": "Nam Từ Liêm",
        "address": "Lê Đức Thọ, Mỹ Đình, Nam Từ Liêm, Hà Nội",
        "pitch_type": "FIVE",
        "base_price": 250000,
        "rating": 4.6,
        "amenities": ["Sân 5 người", "Đèn cao áp", "Gửi xe máy ô tô", "Áo bib", "Nước uống"],
        "description": "Chuyên cụm sân 5 người chất lượng cao, cỏ êm chân hạn chế chấn thương, phù hợp đá giao lưu nội bộ văn phòng sau giờ làm.",
        "image_key": "img_pitch_sunset",
        "is_active": True,
    },
    {
        "id": "p4",
        "name": "Sân vận động Trung Kính",
        "district": "Cầu Giấy",
        "address": "215 Trung Kính, Cầu Giấy, Hà Nội",
        "pitch_type": "FIVE",
        "base_price": 280000,
        "rating": 4.5,
        "amenities": ["Sân 5 người", "Đèn LED", "Tủ đồ cá nhân", "Căn tin"],
        "description": "Vị trí trung tâm thuận tiện di chuyển giữa các quận, sân cỏ được vệ sinh và bảo dưỡng định kỳ hàng tuần.",
        "image_key": "img_pitch_night",
        "is_active": True,
    },
    {
        "id": "p5",
        "name": "Sân bóng Tân Triều Park",
        "district": "Thanh Xuân",
        "address": "Ngõ 300 Nguyễn Xiển, Thanh Xuân, Hà Nội",
        "pitch_type": "ELEVEN",
        "base_price": 900000,
        "rating": 4.9,
        "amenities": ["Sân 11 người", "Khán đài", "Phòng tắm nóng lạnh", "Bãi xe ô tô", "Trọng tài"],
        "description": "Sân bóng 11 người đạt tiêu chuẩn tổ chức các giải đấu phong trào lớn, kích thước tiêu chuẩn FIFA, mặt sân phẳng và cỏ mềm.",
        "image_key": "img_pitch_1",
        "is_active": True,
    },
    {
        "id": "p6",
        "name": "Sân bóng Keangnam Garden",
        "district": "Nam Từ Liêm",
        "address": "Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội",
        "pitch_type": "SEVEN",
        "base_price": 420000,
        "rating": 4.8,
        "amenities": ["Sân 7 người", "Đèn LED", "Cho thuê giày", "Wifi miễn phí", "Bãi xe"],
        "description": "Không gian thoáng mát cạnh tháp Keangnam Landmark, hệ thống lưới chắn an toàn, dịch vụ chuyên nghiệp và thân thiện.",
        "image_key": "img_pitch_2",
        "is_active": True,
    },
]

SLOT_RANGES = [
    ("06:00:00", "07:30:00"),
    ("07:30:00", "09:00:00"),
    ("09:00:00", "10:30:00"),
    ("10:30:00", "12:00:00"),
    ("12:00:00", "13:30:00"),
    ("13:30:00", "15:00:00"),
    ("15:00:00", "16:30:00"),
    ("16:30:00", "18:00:00"),
    ("18:00:00", "19:30:00"),
    ("19:30:00", "21:00:00"),
    ("21:00:00", "22:30:00"),
]


def seed_database():
    print("🚀 Bắt đầu nạp dữ liệu mẫu vào Supabase...")
    
    # 1. Nạp danh mục sân
    print(f"📦 Đang nạp {len(PITCHES)} sân bóng...")
    for pitch in PITCHES:
        supabase.table("pitches").upsert(pitch).execute()
        print(f"   ✔ Đã cập nhật sân: {pitch['name']} ({pitch['district']})")
    
    # 2. Sinh khung giờ cho 7 ngày tới
    now_vn = datetime.now(ZoneInfo("Asia/Ho_Chi_Minh"))
    today = now_vn.date()
    total_slots = 0
    
    print("\n⏰ Đang tự động sinh khung giờ chơi cho 7 ngày tới...")
    for pitch in PITCHES:
        slots_to_upsert = []
        base_price = pitch["base_price"]
        
        for day_offset in range(8):
            target_date = today + timedelta(days=day_offset)
            date_str = target_date.strftime("%Y%m%d")
            
            for idx, (starts_at, ends_at) in enumerate(SLOT_RANGES, start=1):
                slot_id = f"{pitch['id']}_{date_str}_{idx:02d}"
                # Phụ phí khung giờ tối (sau 16:30)
                price = base_price + 50000 if starts_at >= "16:30:00" and starts_at <= "21:00:00" else base_price
                
                slots_to_upsert.append({
                    "id": slot_id,
                    "pitch_id": pitch["id"],
                    "slot_date": target_date.isoformat(),
                    "starts_at": starts_at,
                    "ends_at": ends_at,
                    "price": price,
                    "status": "available",
                })
        
        # Batch upsert theo từng sân
        if slots_to_upsert:
            supabase.table("time_slots").upsert(slots_to_upsert, on_conflict="pitch_id,slot_date,starts_at").execute()
            total_slots += len(slots_to_upsert)
            print(f"   ✔ {pitch['name']}: đã tạo {len(slots_to_upsert)} khung giờ (từ {today} đến {today + timedelta(days=7)})")
            
    print(f"\n🎉 Nạp dữ liệu hoàn tất thành công! Tổng cộng {len(PITCHES)} sân và {total_slots} khung giờ sẵn sàng phục vụ.")


if __name__ == "__main__":
    seed_database()
