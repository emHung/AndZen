# Component Specifications

## PitchCard — thẻ sân

Hiển thị ảnh, tên, khu vực/địa chỉ rút gọn, loại sân, đánh giá nếu có dữ liệu, tiện ích nổi bật, giá/giờ và CTA. Toàn thẻ có thể mở chi tiết, nhưng nút yêu thích (nếu có) phải có vùng bấm riêng. Ảnh lỗi dùng fallback.

## SearchBar / SearchFilters

- Khu vực, ngày chơi, loại sân; trên mobile có thể gom thành nút mở bộ lọc.
- Có giá trị hiện tại, xóa lọc, áp dụng và trạng thái loading.
- Kết quả rỗng cần gợi ý đổi ngày/khu vực hoặc xóa bộ lọc.

## DatePicker

Hiển thị ngày và thứ tiếng Việt; hỗ trợ ngày hiện tại và các ngày được phép đặt. Ngày đã qua bị vô hiệu hóa. Mobile có thể là dải ngày cuộn ngang.

## TimeSlotPicker

Mỗi slot hiển thị khoảng giờ và giá nếu có khác biệt. Trạng thái cần phân biệt bằng màu, nhãn/accessible name và kiểu tương tác. Chỉ một slot được chọn cho một lượt đặt MVP.

## FormField

Có label, hint tùy chọn, trạng thái lỗi cụ thể; không dùng placeholder thay label. Số điện thoại hỗ trợ bàn phím số và kiểm tra định dạng mức cơ bản.

## BookingSummary

Hiển thị sân, ngày, giờ, thời lượng, đơn giá và tổng tạm tính. Tổng tiền phải được tính từ dữ liệu slot, không nhập tay ở nhiều nơi.

## BookingStatusBadge

Trạng thái: sắp tới/đã xác nhận, hoàn thành, đã hủy. Dùng nhãn chữ và màu semantic.

## Button

Variants: primary, secondary, outline, ghost, danger. Có kích thước phù hợp desktop/mobile, focus rõ, disabled/loading không gửi form lặp.

## Dialog / BottomSheet

Dùng cho xác nhận hủy, bộ lọc trên mobile hoặc thông báo ngắn. Đóng bằng nút rõ ràng; Escape hoạt động trên desktop; focus không chạy ra ngoài khi dialog mở.

## Feedback states

Toast chỉ cho phản hồi ngắn. Lỗi cần hành động khắc phục thì đặt ngay cạnh nội dung/field liên quan. Thành công đặt sân nên dùng trang/xác nhận rõ, có mã đặt và nút xem lịch.
