# Design System

## Tính cách giao diện

Thể thao, năng động, đáng tin cậy và thân thiện. Giao diện ưu tiên tìm giờ trống và đặt nhanh; ảnh sân tạo cảm xúc nhưng thông tin giá, địa điểm, giờ chơi phải dễ quét.

## Nguyên tắc

- Mobile-first; một màn hình tập trung một hành động chính.
- Dùng cùng một bộ token trong mọi trang; tránh màu/cỡ chữ tùy hứng.
- Trạng thái luôn có nhãn hoặc biểu tượng, không chỉ dựa vào màu.
- Vùng bấm tối thiểu 44×44 CSS px; trên cảm ứng ưu tiên 48px.
- Nội dung tiếng Việt ngắn gọn, rõ; CTA dùng động từ: “Tìm sân”, “Chọn giờ”, “Xác nhận đặt”.
- Giữ tương phản chữ đủ rõ; không đặt chữ nhỏ lên ảnh rối.

## Layout và khoảng cách

Thang spacing cơ sở 4px: `4, 8, 12, 16, 24, 32, 40, 48, 64`.
- Padding trang: mobile 16px; tablet 24px; desktop 32px.
- Gap giữa card: mobile 12–16px; tablet/desktop 20–24px.
- Nội dung desktop tối đa khoảng 1200px, căn giữa.
- Bo góc: nhỏ 8px, control 12px, card 16px, dialog 20px, pill 999px.
- Bóng nhẹ, dùng có tiết chế; viền trung tính cho phân cấp bề mặt.

## Phân cấp hành động

- Primary: đặt/tìm/xác nhận; màu xanh chính.
- Secondary: thao tác bổ trợ, viền hoặc nền nhạt.
- Tertiary: link hoặc nút chữ.
- Destructive: hủy đặt; luôn có bước xác nhận nếu thao tác khó hoàn tác.

## Trạng thái

Tất cả control có trạng thái mặc định, hover (nếu có chuột), focus-visible, pressed, disabled, loading và error phù hợp. Lịch giờ cần có nhãn “Còn trống”, “Đã đặt”, “Không hoạt động”, cùng khả năng phân biệt khi không nhìn màu.

## Accessibility

- Thứ tự focus theo luồng đọc; focus ring nhìn rõ.
- Form gắn label thật, lỗi liên kết với trường.
- Ảnh sân có alt mô tả; ảnh trang trí dùng alt rỗng.
- Hỗ trợ zoom và cỡ chữ hệ thống, không khóa viewport trên mobile.
- Tôn trọng `prefers-reduced-motion`; animation ngắn và không cản thao tác.
