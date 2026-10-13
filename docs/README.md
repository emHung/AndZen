# Sân Bóng (AndZen) — Tài liệu dự án

Bộ tài liệu này phản ánh chi tiết kiến trúc, đặc tả tính năng và tiêu chuẩn giao diện của ứng dụng Android Native đặt sân bóng **AndZen** (`com.sanbong.app`).

## Danh mục tài liệu

- [SYSTEM.md](file:///d:/AndZen/docs/SYSTEM.md): Kiến trúc hệ thống, công nghệ nền tảng và quy tắc phát triển (Java <= 150 dòng/controller).
- [PRODUCT_SPEC.md](file:///d:/AndZen/docs/PRODUCT_SPEC.md): Đặc tả tính năng, quy tắc nghiệp vụ và các luồng tương tác thực tế trong app.
- [SITEMAP.md](file:///d:/AndZen/docs/SITEMAP.md): Cấu trúc màn hình, Activity, Fragment, Dialog và luồng điều hướng người dùng.
- [COMPONENTS.md](file:///d:/AndZen/docs/COMPONENTS.md): Danh mục các Activity, Fragment, Adapter, Dialog và quy chuẩn giao diện.
- [DESIGN_SYSTEM.md](file:///d:/AndZen/docs/DESIGN_SYSTEM.md): Nguyên tắc giao diện, chuẩn Remix Icons đồng bộ và thành phần Material 3.
- [THEME.md](file:///d:/AndZen/docs/THEME.md): Bảng màu thương hiệu (Emerald Green `#10B981`, Dark Slate `#0F172A`), typography và shapes.
- [TYPOGRAPHY.md](file:///d:/AndZen/docs/TYPOGRAPHY.md): Quy chuẩn cỡ chữ, độ đậm và hierarchy trên Android.
- [RESPONSIVE.md](file:///d:/AndZen/docs/RESPONSIVE.md): Tương thích đa kích thước màn hình Android (Phone, Tablet, Cụm cuộn ngang).
- [TEST_PLAN.md](file:///d:/AndZen/docs/TEST_PLAN.md): Kịch bản kiểm thử đơn vị và nghiệm thu giao diện trên thiết bị thật.

## Tổng quan ứng dụng

**AndZen** là ứng dụng Android native giúp người dùng tìm kiếm sân bóng đá, lọc theo tiện ích và loại sân (5, 7, 11 người), xem chi tiết khung giờ trống, đặt sân nhanh chóng, quản lý lịch đặt (hủy lịch, đổi lịch), lưu danh sách yêu thích và quản lý hồ sơ cá nhân.

- **Nền tảng**: Android Native (Java 17, Android SDK 34, Min SDK 24).
- **Giao diện**: XML Layouts với Material Design 3, màu sắc hiện đại chuẩn Emerald Green, 100% vector icon từ bộ Remix Icons (`ic_ri_*`).
- **Quản lý dữ liệu**: `BookingRepository` in-memory quản lý danh sách sân, khung giờ và các lịch đặt theo thời gian thực (real-time state across screens).
- **Chất lượng code**: Kiểm soát độ dài file controller $\le 150$ dòng, tách biệt view logic và adapters, unit test đầy đủ cho tính toán tiền tệ và validation.
