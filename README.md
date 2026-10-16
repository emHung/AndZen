# ⚽ AndZen — Ứng Dụng Đặt Sân Bóng Đá Android Native

<p align="center">
  <b>Hệ thống đặt sân bóng thông minh, trực quan, hỗ trợ quản lý lịch đặt & quản trị sân theo thời gian thực</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_Native-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Java_17-orange.svg" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Material_Design_3-blue.svg" alt="Material Design 3" />
  <img src="https://img.shields.io/badge/Icons-Remix_Icons-emerald.svg" alt="Remix Icons" />
  <img src="https://img.shields.io/badge/Backend-FastAPI_+_Supabase-teal.svg" alt="Backend" />
</p>

---

## 📱 Hình Ảnh Giao Diện Thực Tế (Screenshots)

Tất cả hình ảnh dưới đây được chụp trực tiếp từ môi trường chạy thực tế trên Android Emulator:

| 1. Trang chủ / Tìm sân | 2. Chi tiết sân bóng | 3. Xác nhận đặt sân |
|:---:|:---:|:---:|
| <img src="docs/screenshots/trangchu.png" width="240" alt="Trang chủ / Tìm sân" /> | <img src="docs/screenshots/chitietsan.png" width="240" alt="Chi tiết sân bóng" /> | <img src="docs/screenshots/xacnhandatsan.png" width="240" alt="Xác nhận đặt sân" /> |
| **Trang chủ (`trangchu.png`)**<br/>Tìm kiếm sân, lọc loại sân 5/7/11, sân gần bạn | **Chi tiết sân (`chitietsan.png`)**<br/>Tiện ích, dải chọn ngày & lưới khung giờ trống | **Xác nhận (`xacnhandatsan.png`)**<br/>Tóm tắt đơn, thông tin người đặt & phương thức trả |

| 4. Đặt sân thành công | 5. Danh sách lịch đã đặt | 6. Chi tiết đơn đã đặt |
|:---:|:---:|:---:|
| <img src="docs/screenshots/datsanthanhcong.png" width="240" alt="Đặt sân thành công" /> | <img src="docs/screenshots/lichdadat.png" width="240" alt="Lịch đã đặt" /> | <img src="docs/screenshots/chitietsandadat.png" width="240" alt="Chi tiết đơn đã đặt" /> |
| **Thành công (`datsanthanhcong.png`)**<br/>Mã đơn đặt sân duy nhất, chuyển nhanh tới lịch | **Lịch của tôi (`lichdadat.png`)**<br/>Phân loại Sắp tới / Đã xong / Đã hủy | **Chi tiết (`chitietsandadat.png`)**<br/>Thông tin đơn đặt, thao tác đổi lịch & hủy sân |

| 7. Quản lý sân (Admin) | 8. Thông báo hệ thống | 9. Hồ sơ cá nhân |
|:---:|:---:|:---:|
| <img src="docs/screenshots/quanlysan.png" width="240" alt="Quản lý sân bóng" /> | <img src="docs/screenshots/thongbao.png" width="240" alt="Thông báo" /> | <img src="docs/screenshots/profile.png" width="240" alt="Hồ sơ cá nhân" /> |
| **Quản trị (`quanlysan.png`)**<br/>Bảng điều khiển Admin, khóa/mở khung giờ sân | **Thông báo (`thongbao.png`)**<br/>Cập nhật đơn đặt, lời nhắc trước giờ thi đấu | **Tài khoản (`profile.png`)**<br/>Quản lý thông tin, đổi avatar & thống kê số trận |

---

## 🌟 Tính Năng Nổi Bật

1. **Tìm kiếm & Lọc sân bóng**:
   - Tìm kiếm nhanh theo tên sân, khu vực hoặc quận huyện.
   - Bộ lọc chi tiết theo loại sân (5, 7, 11 người), khoảng giá và tiện ích (Đèn LED, Bãi xe, Phòng thay đồ, Nước uống, Áo bib).
2. **Lọc khung giờ thông minh**:
   - Tự động nhận diện những ngày còn khung giờ trống (`AVAILABLE`).
   - Lưới giờ tự động ẩn/lọc các slot đã được đặt, ngăn chặn đặt trùng lặp.
3. **Quy trình đặt sân & Thanh toán**:
   - Đặt sân nhanh chỉ với 2 bước: Chọn giờ $\to$ Xác nhận thông tin.
   - Hỗ trợ thanh toán tiền mặt tại sân hoặc chuyển khoản ngân hàng qua mã QR.
4. **Quản lý lịch đặt (Booking Management)**:
   - Theo dõi trạng thái đơn đặt: Sắp tới, Đã hoàn thành, Đã hủy.
   - Hỗ trợ đổi giờ chơi (Reschedule) sang khung giờ trống mới trực tiếp trên app.
   - Hỗ trợ hủy đơn đặt kèm lý do và tự động giải phóng slot sân.
5. **Giao diện Quản trị viên (Admin)**:
   - Dành riêng cho chủ sân / nhân viên quản lý.
   - Quản lý danh mục sân, bật/tắt trạng thái hoạt động, khóa/mở khung giờ linh hoạt.

---

## 🚀 Hướng Dẫn Cài Đặt Môi Trường & Chạy Dự Án

### 1. Yêu cầu môi trường (Prerequisites)
- **Java**: JDK 17 (khuyên dùng OpenJDK 17 hoặc Oracle JDK 17).
- **Android Studio**: Android Studio Ladybug hoặc mới hơn (Android SDK 34/36).
- **Python**: Python 3.11+ (cho Backend FastAPI).
- **Thiết bị chạy**: Máy thật Android hoặc Android Emulator (API 24+).
- **Git**: Đã cài đặt trên máy.

---

### 2. Cài đặt & Khởi chạy Backend (FastAPI + Supabase)

#### Bước 2.1: Chuẩn bị môi trường Python
Mở terminal và di chuyển vào thư mục `backend/`:
```bash
cd backend
python -m venv .venv
```

Kích hoạt môi trường ảo:
- Trên Windows PowerShell:
  ```powershell
  .\.venv\Scripts\Activate.ps1
  ```
- Trên Linux / macOS:
  ```bash
  source .venv/bin/activate
  ```

Cài đặt các thư viện phụ thuộc:
```bash
pip install -r requirements.txt
```

#### Bước 2.2: Cấu hình biến môi trường
Tạo file `backend/.env` dựa theo file mẫu `backend/.env.example`:
```env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-publishable-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
PORT=8011
```

#### Bước 2.3: Khởi chạy máy chủ API
* **Cách 1 (Nhanh nhất trên Windows qua PowerShell script)**:
  Script này tự động cấu hình `adb reverse` để máy ảo kết nối được với backend và chạy Uvicorn:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\run-local.ps1
  ```
* **Cách 2 (Chạy thủ công bằng Uvicorn)**:
  ```bash
  uvicorn app.main:app --host 127.0.0.1 --port 8011 --reload
  ```
  Sau khi khởi chạy, bạn có thể kiểm tra API tại: `http://127.0.0.1:8011/docs` (Swagger UI).

---

### 3. Cài đặt & Khởi chạy Ứng dụng Android

#### Bước 3.1: Mở dự án trong Android Studio
1. Khởi động Android Studio.
2. Chọn **Open** và duyệt tới thư mục gốc của dự án `d:\AndZen`.
3. Chờ Gradle đồng bộ (Sync) xong dependencies.

#### Bước 3.2: Cấu hình `local.properties`
Tạo hoặc cập nhật file `local.properties` tại thư mục gốc với nội dung:
```properties
sdk.dir=C\:\\Users\\<Tên_User>\\AppData\\Local\\Android\\Sdk
sanbong.supabaseUrl=https://your-project.supabase.co
sanbong.supabasePublishableKey=your-publishable-key
sanbong.apiBaseUrl=http://127.0.0.1:8000
```

#### Bước 3.3: Kết nối mạng giữa Emulator và Backend
Nếu chạy thử nghiệm trên Android Emulator, mở terminal chạy lệnh chuyển tiếp cổng:
```bash
adb reverse tcp:8000 tcp:8011
```
*(Lệnh này giúp ứng dụng Android gọi `http://127.0.0.1:8000` được định tuyến thẳng tới port `8011` của Backend trên máy tính).*

#### Bước 3.4: Biên dịch và cài đặt ứng dụng
- **Trong Android Studio**: Nhấn nút **Run** (hoặc tổ hợp phím `Shift + F10`).
- **Hoặc qua dòng lệnh (Terminal)**:
  ```bash
  # Build bản Debug
  ./gradlew assembleDebug

  # Cài đặt trực tiếp lên Emulator/Thiết bị
  ./gradlew installDebug
  ```

---

### 4. Kiểm Thử Tự Động (Running Tests)

* **Chạy Unit Test phía Android**:
  ```bash
  ./gradlew test
  ```
* **Chạy Contract Test phía Backend**:
  ```bash
  cd backend
  python -m unittest tests/test_contracts.py
  ```

---

## 🛠️ Kiến Trúc & Công Nghệ

- **Client**: Android Native (Java 17), Android SDK 34/36, Min SDK 24.
- **UI System**: Material Design 3, màu thương hiệu Emerald Green (`#10B981`), 100% vector icon từ Remix Icons (`ic_ri_*`).
- **Quy chuẩn mã nguồn**: Tất cả Activity và Controller tuân thủ giới hạn $\le 150$ dòng code, tách biệt rành mạch giữa UI Controller, Adapter và Repository/API Client.
- **Backend API**: FastAPI (Python 3.14) chạy Uvicorn, bảo mật bằng JWT và Row-Level Security trên Supabase PostgreSQL.

---

## 📖 Danh Mục Tài Liệu Chi Tiết

Xem thêm các tài liệu kỹ thuật chuyên đề trong thư mục [`docs/`](docs/):
- [`docs/SITEMAP.md`](docs/SITEMAP.md): Cây điều hướng màn hình và danh mục API endpoints.
- [`docs/SYSTEM.md`](docs/SYSTEM.md): Kiến trúc hệ thống và quy tắc coding chuẩn.
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md): Đặc tả nghiệp vụ và sản phẩm chi tiết.
- [`docs/COMPONENTS.md`](docs/COMPONENTS.md): Danh mục các Activity, Fragment, Dialog và Adapter.
- [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md): Hệ thống thiết kế, màu sắc và icon.
- [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md): Kịch bản kiểm thử và nghiệm thu.
