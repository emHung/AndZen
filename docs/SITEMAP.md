# Sitemap

## Cấu trúc điều hướng người dùng

```text
Ứng dụng
├── Trang chủ / Tìm sân
│   ├── Chọn khu vực
│   ├── Chọn ngày chơi
│   ├── Bộ lọc (loại sân, mức giá, tiện ích)
│   ├── Danh sách sân
│   └── Chi tiết sân
│       ├── Ảnh và thông tin sân
│       ├── Tiện ích, địa chỉ, giá
│       ├── Lịch và khung giờ
│       └── Đặt sân
│           ├── Thông tin người đặt
│           ├── Xem lại đặt chỗ
│           └── Đặt thành công
├── Lịch của tôi
│   ├── Sắp tới
│   ├── Đã hoàn thành
│   ├── Đã hủy
│   └── Chi tiết đặt sân
│       └── Hủy đặt (nếu đủ điều kiện)
├── Yêu thích (có thể để sau MVP)
├── Thông báo (có thể để sau MVP)
└── Tài khoản (demo / có thể để sau MVP)
    ├── Hồ sơ
    └── Cài đặt
```

## Điều hướng MVP

- Mobile: thanh điều hướng dưới gồm **Tìm sân**, **Lịch của tôi**, **Tài khoản**. Mục chưa triển khai phải được ẩn hoặc hiển thị trạng thái “Sắp có”, không để link hỏng.
- Tablet/desktop: thanh điều hướng đầu trang; mục chính **Tìm sân**, **Lịch của tôi**; CTA nổi bật **Đặt sân** hoặc **Đăng nhập** chỉ khi phù hợp với chức năng đã có.
- Chi tiết sân và xác nhận đặt phải có đường quay lại rõ ràng, giữ ngày/khu vực đã chọn khi quay lại.

## URL gợi ý cho web

```text
/                  Trang chủ/tìm sân
/pitches           Danh sách sân (nếu tách riêng)
/pitches/:id       Chi tiết sân
/booking/:pitchId  Chọn slot và thông tin đặt
/booking/success   Đặt thành công
/my-bookings       Lịch của tôi
/my-bookings/:id   Chi tiết đặt
```

Nếu dự án là app native, có thể dùng route tương đương theo router hiện có; không cần ép dùng URL web.
