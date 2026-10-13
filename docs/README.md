# Sân Bóng — Tài liệu dự án

Bộ tài liệu này là nguồn yêu cầu cho Antigravity khi thiết kế, lập trình và kiểm thử ứng dụng đặt sân bóng. Đọc `SYSTEM.md` trước khi làm; các file chuyên đề bên dưới là tiêu chuẩn thống nhất.

## Tài liệu

- `SYSTEM.md`: cách agent làm việc và quy tắc triển khai.
- `PRODUCT_SPEC.md`: phạm vi MVP, tính năng và nghiệp vụ.
- `SITEMAP.md`: cấu trúc trang và điều hướng.
- `DESIGN_SYSTEM.md`: nguyên tắc giao diện, component và trạng thái.
- `THEME.md`: bảng màu và cách áp dụng theme.
- `TYPOGRAPHY.md`: font và thang chữ.
- `RESPONSIVE.md`: bố cục desktop, tablet, mobile.
- `COMPONENTS.md`: đặc tả component giao diện.
- `TEST_PLAN.md`: kịch bản và tiêu chí nghiệm thu.

## Hướng sản phẩm

Ứng dụng giúp người chơi tìm sân bóng, xem giá và giờ trống, đặt lịch, rồi quản lý lịch đặt. MVP dùng dữ liệu demo; chưa tích hợp thanh toán thật. Thiết kế ưu tiên tiếng Việt, mobile-first và khả năng mở rộng sang quản trị sân.

## Nguyên tắc thực hiện

1. Đọc toàn bộ tài liệu liên quan trước khi sửa.
2. Xây luồng đặt sân xuyên suốt trước, sau đó mới mở rộng tính năng.
3. Dùng dữ liệu demo có cấu trúc rõ ràng; không tạo trạng thái giao diện giả không đồng bộ với dữ liệu.
4. Sau mỗi tính năng, chạy app, kiểm tra console và thử trên các kích thước trong `RESPONSIVE.md`.
5. Không tự thêm thanh toán, đăng nhập thật, backend hoặc tích hợp bên thứ ba khi chưa có yêu cầu.
