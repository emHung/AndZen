# Responsive — Desktop, Tablet, Mobile

Thiết kế mobile-first. Breakpoint dưới đây là điểm bắt đầu; điều chỉnh theo nội dung và framework hiện tại, không để breakpoint làm tràn hoặc chồng nội dung.

## Breakpoint tham chiếu

| Tên | Kích thước viewport | Quy tắc |
|---|---:|---|
| Mobile nhỏ | 320–374px | Một cột, điều khiển gọn, không cuộn ngang |
| Mobile | 375–767px | Một cột; điều hướng dưới |
| Tablet | 768–1023px | Danh sách 2 cột khi card đủ rộng |
| Desktop | từ 1024px | Lưới 3 cột trở lên theo chiều rộng |
| Desktop rộng | từ 1280px | Giới hạn chiều rộng nội dung; không kéo card quá dài |

## Mobile

- Ưu tiên thao tác bằng một tay; nội dung chính một cột.
- Header gọn: logo/tên, vị trí hoặc tài khoản; tránh nav ngang dài.
- Thanh điều hướng dưới cho các mục chính; chừa safe area phía dưới.
- Bộ lọc mở dạng bottom sheet hoặc màn hình riêng; có nút “Áp dụng” và “Xóa lọc”.
- Card sân xếp dọc; ảnh có tỷ lệ ổn định, không chiếm quá nhiều chiều cao.
- Lịch ngày có thể cuộn ngang; slot giờ xếp lưới 2 cột hoặc 3 cột tùy bề rộng.
- Trang chi tiết đặt CTA ở cuối nội dung; có thể sticky ở đáy nếu không che thông tin/keyboard.
- Form có input full-width, bàn phím số cho số điện thoại; không đặt hai trường hẹp cạnh nhau.
- Dialog trên màn hình nhỏ dùng gần full-screen hoặc bottom sheet.
- Không cuộn ngang ở cấp trang; bảng dữ liệu nếu có phải chuyển thành card.

## Tablet

- Padding trang 24px.
- Lưới danh sách sân 2 cột; nếu màn hình dọc hoặc vùng nội dung hẹp thì tự chuyển 1 cột.
- Bộ lọc hiển thị thành hàng ngang hoặc panel mở rộng; không chiếm quá nhiều chiều ngang.
- Trang chi tiết có thể chia 2 cột: ảnh/thông tin và khối lịch/đặt, chỉ khi mỗi cột đủ rộng.
- Thanh điều hướng trên cùng; có thể dùng drawer nếu nhiều mục.
- Kiểm tra cả tablet dọc và ngang.

## Desktop

- Nội dung căn giữa, max-width khoảng 1200px.
- Danh sách sân 3 cột ở vùng đủ rộng; 4 cột chỉ khi card vẫn đọc tốt.
- Bộ lọc có thể ở sidebar trái hoặc hàng filter phía trên.
- Chi tiết sân dùng 2–3 vùng: ảnh/thông tin, lịch giờ, tóm tắt đặt.
- Hover chỉ bổ trợ; mọi hành động vẫn phải dùng được bằng bàn phím.

## Kiểm tra tối thiểu

Kiểm tra viewport 320×568, 375×812, 390×844, 768×1024, 820×1180, 1024×768 và 1440×900. Kiểm tra dọc/ngang trên tablet, zoom 200%, bàn phím mở trong form và safe area trên điện thoại.
