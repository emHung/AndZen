# Product Specification

## Mục tiêu

Giúp người chơi tìm sân phù hợp và đặt nhanh một khung giờ còn trống. MVP là ứng dụng demo tiếng Việt, responsive, có thể chạy trên trình duyệt và/hoặc Android emulator tùy stack hiện tại.

## Người dùng chính

- Người chơi muốn tìm sân gần khu vực mình.
- Nhóm trưởng cần đặt sân và chia sẻ thông tin lịch.
- Chủ sân (ngoài MVP) cần quản lý sân, giá và lịch trống.

## Phạm vi MVP

- Trang chủ/tìm sân với khu vực, ngày chơi và loại sân.
- Danh sách sân, lọc/sắp xếp cơ bản.
- Chi tiết sân: ảnh, tên, địa chỉ, loại sân, tiện ích, giá, mô tả.
- Chọn ngày và khung giờ trống.
- Nhập thông tin người đặt tối thiểu: họ tên, số điện thoại.
- Xác nhận đặt demo, hiển thị mã đặt và tổng tiền.
- Danh sách lịch đặt; xem chi tiết và hủy lịch demo.
- Dữ liệu demo có trạng thái khung giờ rõ ràng.

## Chưa thuộc MVP

Thanh toán thật, xác thực OTP, chat, đánh giá có kiểm duyệt, định vị nền, thông báo SMS/push, hệ thống chủ sân, nhiều vai trò, đồng bộ backend.

## Luồng chính

1. Người dùng mở tìm sân.
2. Chọn ngày, khu vực hoặc loại sân.
3. Xem danh sách, mở chi tiết sân.
4. Chọn ngày và một khung giờ còn trống.
5. Điền thông tin người đặt, xem lại giá và xác nhận.
6. Nhận màn hình thành công với mã đặt.
7. Có thể xem lịch đặt trong mục “Lịch của tôi” và hủy theo quy tắc demo.

## Quy tắc nghiệp vụ

- Loại sân: 5 người, 7 người, 11 người; có thể bổ sung loại khác sau.
- Mỗi slot có `id`, `pitchId`, `date`, `startTime`, `endTime`, `price`, `status`.
- Trạng thái slot: `available`, `booked`, `unavailable`.
- Không cho xác nhận slot nếu trạng thái không còn `available`.
- Xác nhận đặt tạo bản ghi với mã dễ đọc, thông tin sân, ngày giờ, giá tại thời điểm đặt và trạng thái `confirmed`.
- Hủy đặt demo chuyển sang `cancelled`; slot được mở lại nếu chính sách demo cho phép.
- Hiển thị giá bằng VND, định dạng theo tiếng Việt; không hiển thị số thập phân nếu không cần.
- Ngày giờ lưu và hiển thị nhất quán theo múi giờ Việt Nam.

## Dữ liệu mẫu tối thiểu

Tạo ít nhất 6 sân ở 2–3 khu vực, đa dạng loại sân và mức giá. Mỗi sân có ảnh minh họa, nhưng cần fallback nếu ảnh lỗi. Tạo slot cho hôm nay và 7 ngày tiếp theo, gồm đủ trạng thái trống/đã đặt/không khả dụng.

## Tiêu chí hoàn thành MVP

- Hoàn tất luồng tìm → xem → chọn slot → xác nhận → xem lịch.
- Trạng thái slot cập nhật sau đặt/hủy mà không cần tải lại toàn bộ ứng dụng.
- Không có CTA chính nào chết hoặc dẫn đến trang trống.
- Có trạng thái rỗng và thông báo lỗi dễ hiểu.
- Responsive theo `RESPONSIVE.md`, pass kịch bản cốt lõi trong `TEST_PLAN.md`.
