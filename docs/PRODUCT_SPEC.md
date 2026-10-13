# Đặc tả Sản phẩm (Product Specification)

## 1. Mục tiêu sản phẩm

**AndZen** là ứng dụng di động Android native phục vụ cộng đồng người chơi bóng đá phong trào tại Việt Nam. Ứng dụng cung cấp giải pháp tìm kiếm sân bóng theo khu vực và tiện ích, xem trạng thái lịch trống theo thời gian thực, đặt sân nhanh chóng và quản lý toàn diện lịch đặt (hủy lịch, đổi lịch).

## 2. Đối tượng người dùng

- **Người đặt sân/Đội trưởng**: Cần tra cứu nhanh sân trống, kiểm tra giá, đặt slot và chia sẻ thông tin trận đấu cho cả đội.
- **Cầu thủ**: Theo dõi lịch thi đấu cá nhân, lưu danh sách sân bóng yêu thích và nhận thông báo cập nhật lịch.

## 3. Các tính năng cốt lõi đã triển khai

### 3.1. Xác thực & Tài khoản (`AuthActivity`, `ProfileFragment`, `EditProfileActivity`)
- Đăng nhập / Đăng ký qua Email, Số điện thoại hoặc Google.
- Kiểm tra hợp lệ dữ liệu nhập (ValidationUtils): định dạng email, độ dài số điện thoại VN, mật khẩu.
- Chỉnh sửa hồ sơ cá nhân: Họ tên, số điện thoại, email, địa chỉ, ngày sinh (sử dụng DatePicker giao diện sáng rõ ràng).
- Quản lý trạng thái đăng xuất và lưu trữ profile in-memory.

### 3.2. Tìm kiếm & Danh mục sân bóng (`PitchListFragment`, `PitchFilterActivity`)
- Danh sách sân bóng phong phú với hình ảnh chất lượng cao (không dính watermark hay icon đè), tên sân, địa chỉ, đánh giá sao, loại sân (5, 7, 11 người).
- Bộ lọc nâng cao theo:
  - Loại sân: Sân 5, Sân 7, Sân 11.
  - Khoảng giá: Dưới 300.000đ, 300.000đ - 500.000đ, Trên 500.000đ.
  - Tiện ích: Đèn LED, Bãi gửi xe, Phòng thay đồ, Nước uống miễn phí, Căn tin, Áo bib thi đấu.
- Tìm kiếm theo từ khóa tên sân và khu vực.

### 3.3. Chi tiết sân bóng & Lọc lịch thông minh (`PitchDetailActivity`)
- Hiển thị 4 hình ảnh sân thực tế (`img_pitch_1.png`, `img_pitch_2.png`, `img_pitch_night.png`, `img_pitch_sunset.png`).
- Nút thêm/bỏ yêu thích nhanh chóng ngay trên thanh tiêu đề.
- **Cơ chế lọc lịch thông minh**:
  - Dải chọn ngày cuộn ngang (`DateChipAdapter`): Tự động tính toán và đánh dấu những ngày có ít nhất một khung giờ còn trống (`AVAILABLE`), hỗ trợ người dùng nhận biết ngay ngày nào có thể đặt.
  - Lưới khung giờ (`TimeSlotAdapter`): Lọc và chỉ hiển thị các khung giờ chưa có người đặt (loại bỏ các khung giờ đã `BOOKED` hoặc `UNAVAILABLE`), giúp giao diện gọn gàng và tránh đặt trùng lặp.
- Thanh Bottom Bar ghim cố định hiển thị tổng tiền theo slot được chọn và nút "Đặt sân ngay".

### 3.4. Quy trình đặt sân & Xác nhận (`BookingConfirmActivity`, `BookingSuccessActivity`)
- Tóm tắt đơn đặt gồm tên sân, ngày đá, giờ đá và tổng tiền format tiếng Việt (VD: `350.000 đ`).
- Nhập thông tin người nhận sân và ghi chú.
- Lựa chọn phương thức thanh toán: Tiền mặt tại sân hoặc Chuyển khoản QR.
- Màn hình xác nhận thành công với mã đặt sân duy nhất (`SANBONG-XXXXXX`) và điều hướng tức thì tới danh sách lịch đặt.

### 3.5. Quản lý lịch đặt (`MyBookingsFragment`, `BookingDetailActivity`)
- 3 tab trạng thái: **Sắp tới** (Upcoming), **Đã hoàn thành** (Completed), **Đã hủy** (Cancelled).
- Thẻ lịch đặt hiển thị đầy đủ ngày giờ, giá tiền, trạng thái semantic badge.
- **Hủy lịch đặt sân**: Hỗ trợ người dùng hủy lịch với dialog xác nhận lý do; khi hủy, slot sân được tự động giải phóng trở lại trạng thái `AVAILABLE`.
- **Đổi lịch đặt sân (Reschedule)**: Cho phép chọn ngày mới và khung giờ trống mới trực tiếp qua dialog `dialog_reschedule_slot.xml`, tự động cập nhật slot cũ thành trống và đánh dấu slot mới thành đã đặt.

### 3.6. Danh sách yêu thích (`FavoritesFragment`)
- Quản lý các sân bóng được đánh dấu yêu thích.
- Đồng bộ hai chiều với nút tim trên `PitchDetailActivity`.
- Trạng thái trống (Empty State) với hình ảnh minh họa thân thiện.

### 3.7. Thông báo (`NotificationActivity`)
- Danh sách thông báo cập nhật trạng thái đơn đặt sân, thông báo khuyến mãi và lời nhắc trận đấu.
- Phân loại rõ ràng kèm thời gian tương đối.

## 4. Bất biến nghiệp vụ (Business Invariants)

1. **Tính đơn nhất của khung giờ**: Tại một thời điểm, một slot của sân bóng chỉ có duy nhất một đơn đặt ở trạng thái `CONFIRMED`.
2. **Giải phóng slot khi hủy**: Khi đơn đặt bị hủy, `BookingRepository` lập tức chuyển trạng thái của slot tương ứng về `AVAILABLE`.
3. **Chuyển giao slot khi đổi lịch**: Khung giờ cũ được mở lại (`AVAILABLE`) và khung giờ mới được chuyển thành `BOOKED`.
4. **Quy chuẩn hiển thị giá**: Tất cả số tiền đều qua `CurrencyUtils.formatVnd(...)` đảm bảo định dạng số có dấu chấm và đơn vị tiền tệ chuẩn Việt Nam.
