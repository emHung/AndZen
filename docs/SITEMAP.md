# Sitemap & Cấu trúc Điều hướng Ứng dụng

## Cấu trúc màn hình Android Native

```text
com.sanbong.app
├── AuthActivity (Đăng nhập / Đăng ký)
│   ├── Tab Đăng nhập (Email/SĐT + Mật khẩu / Google)
│   └── Tab Đăng ký (Họ tên, SĐT, Email, Mật khẩu)
│
├── MainActivity (Màn hình chính - BottomNavigationView 4 Tabs)
│   │
│   ├── Tab 1: PitchListFragment (Tìm kiếm & Danh sách sân)
│   │   ├── Thanh tìm kiếm & Nút mở Bộ lọc -> PitchFilterActivity
│   │   ├── Nút chuông thông báo -> NotificationActivity
│   │   ├── Chip lọc nhanh loại sân (Tất cả, Sân 5, Sân 7, Sân 11)
│   │   └── Danh sách sân (PitchAdapter) -> Mở PitchDetailActivity
│   │
│   ├── Tab 2: FavoritesFragment (Sân bóng yêu thích)
│   │   ├── Danh sách thẻ sân đã thích (FavoritePitchAdapter)
│   │   ├── Bỏ yêu thích trực tiếp trên thẻ
│   │   └── Trạng thái rỗng khi chưa có sân yêu thích
│   │
│   ├── Tab 3: MyBookingsFragment (Quản lý lịch đặt)
│   │   ├── Tab phân loại: Sắp tới (Upcoming) / Đã hoàn thành (Completed) / Đã hủy (Cancelled)
│   │   ├── Danh sách thẻ lịch đặt (BookingAdapter) -> Mở BookingDetailActivity
│   │   └── Trạng thái rỗng kèm gợi ý tìm sân
│   │
│   └── Tab 4: ProfileFragment (Tài khoản cá nhân)
│       ├── Header thông tin (Avatar, Tên, Số điện thoại, Email)
│       ├── Nút "Chỉnh sửa thông tin" -> Mở EditProfileActivity
│       ├── Thống kê nhanh số trận đã đặt & sân yêu thích
│       ├── Cài đặt thông báo, đổi mật khẩu, chính sách & điều khoản
│       └── Nút Đăng xuất -> Chuyển về AuthActivity
│
├── PitchFilterActivity (Bộ lọc nâng cao)
│   ├── Lọc theo Loại sân (Sân 5, 7, 11 người)
│   ├── Lọc theo Khoảng giá (Dưới 300k, 300k - 500k, Trên 500k)
│   ├── Lọc theo Tiện ích (Đèn LED, Bãi gửi xe, Phòng thay đồ, Nước uống, Căn tin, Áo bib)
│   └── Nút Đặt lại & Áp dụng
│
├── NotificationActivity (Thông báo hệ thống)
│   ├── Danh sách thông báo cập nhật lịch đặt, khuyến mãi
│   └── Đánh dấu đã đọc
│
├── PitchDetailActivity (Chi tiết sân bóng)
│   ├── Hình ảnh sân (4 hình ảnh sạch: img_pitch_1, img_pitch_2, img_pitch_night, img_pitch_sunset)
│   ├── Nút Back và nút Yêu thích (ic_ri_heart / ic_ri_heart_fill)
│   ├── Tên sân, đánh giá sao, địa chỉ đầy đủ, giá tiền/giờ
│   ├── Danh sách tiện ích kèm Remix Icons
│   ├── Dải ngày cuộn ngang (DateChipAdapter) - Tự động lọc ngày chưa đặt & slot còn trống
│   ├── Lưới khung giờ (TimeSlotAdapter) - Chỉ hiển thị slot chưa được đặt
│   └── Thanh Bottom Bar: Tổng tiền tạm tính & Nút "Đặt sân ngay" -> Mở BookingConfirmActivity
│
├── BookingConfirmActivity (Xác nhận đặt sân)
│   ├── Tóm tắt thông tin sân, ngày chơi, khung giờ đã chọn
│   ├── Thông tin người đặt (Họ tên, SĐT, Ghi chú)
│   ├── Lựa chọn phương thức thanh toán (Tiền mặt tại sân / Chuyển khoản ngân hàng)
│   └── Nút "Xác nhận đặt sân" -> Mở BookingSuccessActivity
│
├── BookingSuccessActivity (Đặt sân thành công)
│   ├── Icon checkmark thành công & Mã đặt sân (SANBONG-XXXX)
│   ├── Tóm tắt sân, ngày và giờ đặt
│   ├── Nút "Xem lịch đặt" -> Điều hướng tới MyBookingsFragment
│   └── Nút "Về trang chủ" -> Quay lại PitchListFragment
│
├── BookingDetailActivity (Chi tiết đơn đặt)
│   ├── Thông tin chi tiết mã đặt, trạng thái (Badge màu sắc)
│   ├── Thông tin sân, địa chỉ, ngày giờ, số tiền, phương thức thanh toán
│   ├── Nút "Đổi lịch" (nếu trạng thái Sắp tới) -> Mở DialogRescheduleSlot
│   └── Nút "Hủy đặt sân" (nếu trạng thái Sắp tới) -> Mở DialogCancelBooking
│
├── EditProfileActivity (Chỉnh sửa thông tin cá nhân)
│   ├── Đổi avatar (ic_ri_camera)
│   ├── Form họ tên, số điện thoại, email, địa chỉ
│   ├── Bộ chọn ngày sinh trực quan (Material DatePicker phong cách sáng, dễ nhìn)
│   └── Nút "Lưu thay đổi" -> Cập nhật trực tiếp vào ProfileFragment
│
└── Dialogs & BottomSheets:
    ├── DialogCancelBooking (Xác nhận lý do hủy đơn: Bận việc, Đổi ý, Thời tiết xấu, Khác)
    └── DialogRescheduleSlot (Chọn ngày và khung giờ trống mới để dời lịch)
```
