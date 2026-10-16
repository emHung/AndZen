# AndZen — Tài Liệu Kỹ Thuật Dự Án & Sơ Đồ Hệ Thống

Bộ tài liệu này phản ánh chi tiết kiến trúc, đặc tả tính năng, tiêu chuẩn giao diện và hướng dẫn thiết lập môi trường của ứng dụng Android Native đặt sân bóng **AndZen** (`com.sanbong.app`).

Tài liệu chính thức của toàn bộ dự án với đầy đủ hướng dẫn từng bước và hình ảnh minh họa được duy trì tại: [README.md gốc của dự án](file:///d:/AndZen/README.md).

---

## 📚 Danh Mục Tài Liệu Kỹ Thuật

- [SITEMAP.md](file:///d:/AndZen/docs/SITEMAP.md): Cấu trúc màn hình, Activity, Fragment, Dialog, Admin và danh mục API endpoints.
- [SYSTEM.md](file:///d:/AndZen/docs/SYSTEM.md): Kiến trúc hệ thống, công nghệ nền tảng và quy tắc phát triển (Java <= 150 dòng/controller).
- [PRODUCT_SPEC.md](file:///d:/AndZen/docs/PRODUCT_SPEC.md): Đặc tả tính năng, quy tắc nghiệp vụ và các luồng tương tác thực tế trong app.
- [COMPONENTS.md](file:///d:/AndZen/docs/COMPONENTS.md): Danh mục các Activity, Fragment, Adapter, Dialog và quy chuẩn giao diện.
- [DESIGN_SYSTEM.md](file:///d:/AndZen/docs/DESIGN_SYSTEM.md): Nguyên tắc giao diện, chuẩn Remix Icons đồng bộ và thành phần Material 3.
- [THEME.md](file:///d:/AndZen/docs/THEME.md): Bảng màu thương hiệu (Emerald Green `#10B981`, Dark Slate `#0F172A`), typography và shapes.
- [TYPOGRAPHY.md](file:///d:/AndZen/docs/TYPOGRAPHY.md): Quy chuẩn cỡ chữ, độ đậm và hierarchy trên Android.
- [RESPONSIVE.md](file:///d:/AndZen/docs/RESPONSIVE.md): Tương thích đa kích thước màn hình Android (Phone, Tablet, Cụm cuộn ngang).
- [TEST_PLAN.md](file:///d:/AndZen/docs/TEST_PLAN.md): Kịch bản kiểm thử đơn vị và nghiệm thu giao diện trên thiết bị thật.

---

## 📱 Danh Mục Hình Ảnh Giao Diện Thực Tế

Bộ ảnh chụp màn hình chất lượng cao ghi lại từ thiết bị Android Emulator được lưu trữ trong thư mục [`docs/screenshots/`](file:///d:/AndZen/docs/screenshots/):
- `trangchu.png`: Trang chủ & Tìm kiếm sân bóng
- `chitietsan.png`: Chi tiết sân bóng & Chọn khung giờ
- `xacnhandatsan.png`: Xác nhận thông tin & Thanh toán
- `datsanthanhcong.png`: Đặt sân thành công
- `lichdadat.png`: Danh sách lịch đã đặt
- `chitietsandadat.png`: Chi tiết đơn đã đặt & Thao tác hủy/đổi lịch
- `quanlysan.png`: Quản lý sân bóng (Admin Dashboard)
- `thongbao.png`: Trung tâm thông báo hệ thống
- `profile.png`: Hồ sơ cá nhân & Tài khoản
