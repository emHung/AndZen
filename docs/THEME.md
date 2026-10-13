# Theme và màu sắc

## Theme mặc định: Sân cỏ hiện đại

| Token | Giá trị | Dùng cho |
|---|---:|---|
| `brand-950` | `#062E25` | Chữ/điểm nhấn xanh đậm |
| `brand-800` | `#075E46` | Header/hover đậm |
| `brand-700` | `#087A55` | Nút chính, liên kết quan trọng |
| `brand-600` | `#0B9464` | Accent xanh |
| `brand-100` | `#DDF5E9` | Nền nhấn nhẹ |
| `brand-50` | `#F1FAF5` | Nền xanh rất nhạt |
| `surface` | `#FFFFFF` | Card và vùng nội dung |
| `background` | `#F7F9F7` | Nền trang |
| `text-primary` | `#17211D` | Nội dung chính |
| `text-secondary` | `#5F6F66` | Nội dung phụ |
| `border` | `#DCE5DF` | Viền control/card |
| `success` | `#16834A` | Thành công, slot trống (kèm nhãn) |
| `warning` | `#B76A08` | Cảnh báo |
| `danger` | `#C83E3E` | Lỗi/hủy, slot đã kín (kèm nhãn) |
| `info` | `#2463A6` | Thông tin |

## Màu slot giờ

- Còn trống: nền xanh nhạt, chữ xanh đậm, nhãn “Còn trống”.
- Đang chọn: nền `brand-700`, chữ trắng, dấu chọn rõ.
- Đã đặt: nền xám nhạt, chữ xám đậm, nhãn “Đã đặt”, không tương tác.
- Không khả dụng: nền trung tính, nhãn “Không hoạt động”, không tương tác.
- Lỗi chọn/xung đột: thông báo đỏ kèm giải thích và cách thử lại.

## Theme tối

MVP có thể chỉ dùng theme sáng. Nếu đã có dark mode, dùng token semantic thay vì đảo màu thủ công:

| Token | Dark value |
|---|---:|
| `background` | `#101713` |
| `surface` | `#18211B` |
| `text-primary` | `#F0F5F1` |
| `text-secondary` | `#B1BFB5` |
| `border` | `#33443A` |
| `brand` | `#39C58A` |

Kiểm tra tương phản trước khi phát hành. Không dùng màu thương hiệu cho đoạn chữ dài nếu độ tương phản không đạt.

## Cách triển khai

Định nghĩa token bằng CSS custom properties, theme config hoặc hệ thống hiện có. UI chỉ tham chiếu token semantic như `--color-primary`, `--color-surface`, `--color-text`; tránh rải mã hex khắp component.
