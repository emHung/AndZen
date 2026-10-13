# Test Plan và Acceptance Tests

## Tìm và lọc sân

- Mở trang tìm sân thấy danh sách mẫu hợp lệ.
- Lọc theo ngày/khu vực/loại sân cập nhật kết quả đúng.
- Xóa lọc khôi phục kết quả.
- Không có kết quả hiển thị trạng thái rỗng và gợi ý phù hợp.

## Chi tiết và slot

- Mở card tới đúng chi tiết sân.
- Giá, loại sân, địa chỉ và tiện ích khớp dữ liệu.
- Ngày quá khứ không chọn được.
- Chỉ slot `available` có thể chọn; slot booked/unavailable không thể chọn.
- Chọn slot cập nhật trạng thái selected nhìn thấy rõ.

## Đặt sân

- Thiếu họ tên hoặc số điện thoại: báo lỗi sát trường; không tạo booking.
- Số điện thoại không hợp lệ: hiển thị hướng dẫn sửa.
- Xác nhận với dữ liệu hợp lệ tạo booking và mã đặt.
- Tổng tiền và thông tin xác nhận khớp slot đã chọn.
- Sau khi đặt, slot không còn khả dụng cho booking tiếp theo.
- Nhấn xác nhận nhiều lần không tạo booking trùng.

## Quản lý lịch

- Booking mới xuất hiện trong “Sắp tới”/trạng thái tương ứng.
- Mở chi tiết thấy đúng sân, giờ, mã đặt và giá.
- Hủy yêu cầu xác nhận; chọn không thì giữ nguyên.
- Hủy thành công cập nhật danh sách và slot theo quy tắc demo.

## Responsive và khả năng tiếp cận

- Kiểm tra kích thước trong `RESPONSIVE.md`; không có tràn ngang hoặc CTA bị che.
- Tab bàn phím tới mọi thao tác; focus indicator rõ.
- Label form và thông báo lỗi được đọc đúng.
- Slot không chỉ phân biệt bằng màu.
- Kiểm tra ảnh hỏng, danh sách rỗng và lỗi dữ liệu.

## Thực thi

Dùng lệnh có sẵn trong dự án (`npm run`, `pnpm`, `yarn`, Flutter hoặc lệnh hiện hành). Không thêm framework kiểm thử chỉ để chạy các kiểm tra đơn giản. Nếu có test suite sẵn, chạy suite phù hợp; nếu chưa, kiểm thử thủ công trên emulator/trình duyệt và ghi rõ kết quả.
