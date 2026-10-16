# Giao diện quản lý đã triển khai

Icon của admin dùng Remix Icon. Các icon bổ sung được chuyển từ SVG chính thức sang Android VectorDrawable, nguồn và commit lưu trong `remixicon-sources.json`. Có thể cập nhật bằng `tools/sync_admin_icons.py --apply` (không có `--apply` chỉ xem kế hoạch). Nguồn: https://github.com/Remix-Design/RemixIcon, Remix Icon License v1.0.

Ba mục Lịch đặt, Khung giờ và Nhân viên dùng chung header/tab và theme với Sân bóng.

## Logic

- **Lịch đặt:** thống kê từ các lịch mà tài khoản được phép quản lý; tìm mã/tên/số điện thoại không dấu; lọc kết hợp sân, ngày và trạng thái. Có chi tiết, xác nhận thu đủ tiền và hủy lịch với hộp xác nhận. Không hiển thị hủy sau giờ bắt đầu; không cho thu tiền lịch hủy hoặc đã thu.
- **Khung giờ:** chọn sân và ngày trong hôm nay đến 7 ngày tiếp theo; lưới 2 cột; thống kê theo trạng thái DB, giờ đã qua không tính còn trống. Tạo giờ có chọn giờ, kiểm tra giá, giờ kết thúc và giờ đã qua. Backend tiếp tục chặn trùng/chồng lấn. Khóa/mở có xác nhận; giờ đã đặt chỉ mở lịch tương ứng, không có nút khóa. Liên kết này lọc đúng sân/ngày/khung giờ và lịch đang xác nhận.
- **Nhân viên:** thống kê, tìm tên/email không dấu, lọc đã/chưa phân công; hiển thị sân phụ trách và lưu danh sách sân. Chỉ quản trị viên có mục này; quyền thao tác vẫn được kiểm tra tại backend.
- Các lượt đọc cũ bị bỏ khi đổi tab/ngày/sân. Khi ghi có chặn gửi lặp, đọc lại sau khi lưu và hiển thị lỗi. Không tự retry thao tác ghi.
- Số liệu và tên đều lấy từ API, không dùng số hay tên trong ảnh concept.

## Tổ chức mã

`AdminActivity` điều hướng và giữ biểu mẫu sân. `AdminBookingSection`, `AdminSlotSection`, `AdminStaffSection` phụ trách từng màn. `AdminUi` dùng các màu theme sẵn có, `AdminSection` quản lý tải/lưu và vòng đời.

APK debug ở `app/build/outputs/apk/debug/app-debug.apk`. API local vẫn dùng host 8011, Android 8000 qua ADB reverse. QR chỉ ghi nhận phương thức, không xác nhận giao dịch tự động.
