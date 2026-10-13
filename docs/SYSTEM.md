# Kiến trúc Hệ thống & Quy chuẩn Kỹ thuật (System Architecture)

## 1. Công nghệ & Nền tảng

- **Hệ điều hành mục tiêu**: Android (Min SDK 24 - Android 7.0, Target SDK 34 - Android 14).
- **Ngôn ngữ lập trình**: Java 17.
- **Build System**: Gradle với Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), Version Catalog (`gradle/libs.versions.toml`).
- **Thư viện UI**: AndroidX, Google Material Components (`com.google.android.material:material:1.12.0`).
- **Bộ icon**: 100% Remix Icons Vector Drawables (`app/src/main/res/drawable/ic_ri_*.xml`).

## 2. Quy tắc thiết kế mã nguồn (Coding Standards)

1. **Giới hạn Controller/Activity $\le 150$ dòng code**:
   - Tất cả Activity, Fragment phải tuân thủ nghiêm ngặt giới hạn $\le 150$ dòng.
   - Khi logic mở rộng, phải tách ra các class hỗ trợ (Adapter, Dialog Helper, Repository, Utility functions).
2. **Quản lý dữ liệu tập trung qua Repository**:
   - Mọi thao tác truy vấn, đặt sân, hủy lịch, đổi giờ chơi đều thông qua `BookingRepository`.
   - Dữ liệu in-memory đồng bộ tức thì giữa các màn hình mà không cần refresh phức tạp.
3. **Chuẩn hóa chuỗi & màu sắc trong Resource XML**:
   - Mọi chuỗi giao diện đưa vào `strings.xml`.
   - Màu sắc tập trung tại `colors.xml` tuân theo bảng màu Emerald Design System.
4. **Unit Testing cho Utility**:
   - Các logic tính toán tiền tệ (`CurrencyUtilsTest`), định dạng ngày tháng (`DateTimeUtils`), và kiểm tra tính hợp lệ số điện thoại/email (`ValidationUtilsTest`) đều có unit test tự động đi kèm.

## 3. Lệnh biên dịch & Kiểm thử

- Chạy unit tests:
  ```bash
  ./gradlew test
  ```
- Build APK Debug:
  ```bash
  ./gradlew assembleDebug
  ```
- Cài đặt lên thiết bị:
  ```bash
  ./gradlew installDebug
  ```
