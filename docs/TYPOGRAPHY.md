# Typography

## Font

Ưu tiên `Be Vietnam Pro` cho giao diện tiếng Việt. Dùng font local nếu repo đã có; nếu chưa, có thể dùng system fallback và chỉ thêm font web khi môi trường cho phép.

```css
font-family: "Be Vietnam Pro", Inter, system-ui, -apple-system, "Segoe UI", sans-serif;
```

Không để giao diện phụ thuộc duy nhất vào tải font từ mạng. Trọng số dùng: 400, 500, 600, 700.

## Thang chữ

| Vai trò | Desktop | Mobile | Weight / line-height |
|---|---:|---:|---|
| Display/hero | 40–48px | 30–36px | 700 / 1.15 |
| H1 | 32px | 28px | 700 / 1.2 |
| H2 | 24px | 22px | 600–700 / 1.3 |
| H3 | 20px | 18px | 600 / 1.35 |
| Body large | 18px | 17px | 400 / 1.5 |
| Body | 16px | 16px | 400 / 1.5 |
| Caption | 14px | 14px | 400 / 1.45 |
| Label | 14px | 14px | 500–600 / 1.35 |
| Micro text | 12px | 12px | 500 / 1.35; chỉ dùng phụ trợ |

## Cách dùng

- Tên sân và giá phải có phân cấp nhìn thấy ngay trong card.
- Không dùng quá ba cấp heading trên cùng một màn hình.
- Tránh chữ 12px cho thông tin quan trọng hoặc nút.
- Giá có thể dùng weight 700; giữ format `150.000 ₫/giờ` rõ ràng.
- Cắt dòng tên sân có kiểm soát; không làm vỡ card ở màn hình hẹp.
