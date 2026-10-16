# Sân Bóng (AndZen) — Tài liệu dự án & Giao diện ứng dụng

Bộ tài liệu này phản ánh chi tiết kiến trúc, đặc tả tính năng và tiêu chuẩn giao diện của ứng dụng Android Native đặt sân bóng **AndZen** (`com.sanbong.app`).

---

## 📱 Hình ảnh Giao diện Ứng dụng (App Screenshots)

Dưới đây là hình ảnh thực tế từng màn hình chức năng chính của ứng dụng được chụp trực tiếp từ Emulator:

| 1. Trang chủ / Tìm sân | 2. Chi tiết sân bóng | 3. Xác nhận đặt sân |
|:---:|:---:|:---:|
| <img src="screenshots/trangchu.png" width="240" alt="Trang chủ / Tìm sân" /> | <img src="screenshots/chitietsan.png" width="240" alt="Chi tiết sân bóng" /> | <img src="screenshots/xacnhandatsan.png" width="240" alt="Xác nhận đặt sân" /> |
| **`trangchu.png`**<br/>Danh sách sân bóng, tìm kiếm, lọc nhanh sân 5/7/11 | **`chitietsan.png`**<br/>Thông tin tiện ích, chọn ngày & lưới khung giờ trống | **`xacnhandatsan.png`**<br/>Tóm tắt đơn, thông tin người nhận, hình thức thanh toán |

| 4. Đặt sân thành công | 5. Danh sách lịch đã đặt | 6. Chi tiết đơn đã đặt |
|:---:|:---:|:---:|
| <img src="screenshots/datsanthanhcong.png" width="240" alt="Đặt sân thành công" /> | <img src="screenshots/lichdadat.png" width="240" alt="Lịch đã đặt" /> | <img src="screenshots/chitietsandadat.png" width="240" alt="Chi tiết đơn đã đặt" /> |
| **`datsanthanhcong.png`**<br/>Thông báo thành công, mã đặt sân duy nhất | **`lichdadat.png`**<br/>Phân loại Sắp tới / Đã xong / Đã hủy | **`chitietsandadat.png`**<br/>Chi tiết booking, hỗ trợ đổi lịch & hủy sân |

| 7. Quản lý sân (Admin) | 8. Thông báo hệ thống | 9. Hồ sơ cá nhân |
|:---:|:---:|:---:|
| <img src="screenshots/quanlysan.png" width="240" alt="Quản lý sân bóng" /> | <img src="screenshots/thongbao.png" width="240" alt="Thông báo" /> | <img src="screenshots/profile.png" width="240" alt="Hồ sơ cá nhân" /> |
| **`quanlysan.png`**<br/>Quản trị danh mục sân, khóa/mở khung giờ chơi | **`thongbao.png`**<br/>Thông báo cập nhật đơn đặt, nhắc giờ thi đấu | **`profile.png`**<br/>Thông tin tài khoản, thống kê số trận & sân thích |

---

## 📚 Danh mục tài liệu kỹ thuật

- [SYSTEM.md](file:///d:/AndZen/docs/SYSTEM.md): Kiến trúc hệ thống, công nghệ nền tảng và quy tắc phát triển (Java <= 150 dòng/controller).
- [PRODUCT_SPEC.md](file:///d:/AndZen/docs/PRODUCT_SPEC.md): Đặc tả tính năng, quy tắc nghiệp vụ và các luồng tương tác thực tế trong app.
- [SITEMAP.md](file:///d:/AndZen/docs/SITEMAP.md): Cấu trúc màn hình, Activity, Fragment, Dialog và luồng điều hướng người dùng.
- [COMPONENTS.md](file:///d:/AndZen/docs/COMPONENTS.md): Danh mục các Activity, Fragment, Adapter, Dialog và quy chuẩn giao diện.
- [DESIGN_SYSTEM.md](file:///d:/AndZen/docs/DESIGN_SYSTEM.md): Nguyên tắc giao diện, chuẩn Remix Icons đồng bộ và thành phần Material 3.
- [THEME.md](file:///d:/AndZen/docs/THEME.md): Bảng màu thương hiệu (Emerald Green `#10B981`, Dark Slate `#0F172A`), typography và shapes.
- [TYPOGRAPHY.md](file:///d:/AndZen/docs/TYPOGRAPHY.md): Quy chuẩn cỡ chữ, độ đậm và hierarchy trên Android.
- [RESPONSIVE.md](file:///d:/AndZen/docs/RESPONSIVE.md): Tương thích đa kích thước màn hình Android (Phone, Tablet, Cụm cuộn ngang).
- [TEST_PLAN.md](file:///d:/AndZen/docs/TEST_PLAN.md): Kịch bản kiểm thử đơn vị và nghiệm thu giao diện trên thiết bị thật.

---

## ⚙️ Tổng quan công nghệ

**AndZen** là ứng dụng Android native giúp người dùng tìm kiếm sân bóng đá, lọc theo tiện ích và loại sân (5, 7, 11 người), xem chi tiết khung giờ trống, đặt sân nhanh chóng, quản lý lịch đặt (hủy lịch, đổi lịch), lưu danh sách yêu thích và quản lý hồ sơ cá nhân.

- **Nền tảng**: Android Native (Java 17, Android SDK 34/36, Min SDK 24).
- **Backend API**: FastAPI (Python 3.14) + Supabase PostgreSQL tích hợp Row-Level Security (RLS).
- **Giao diện**: XML Layouts với Material Design 3, màu sắc hiện đại chuẩn Emerald Green, 100% vector icon từ bộ Remix Icons (`ic_ri_*`).
- **Chất lượng code**: Kiểm soát độ dài file controller $\le 150$ dòng, tách biệt view logic và adapters, unit test đầy đủ cho tính toán tiền tệ, validation và API contracts.
