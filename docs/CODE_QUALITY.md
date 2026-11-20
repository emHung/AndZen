# Kiểm tra code AndZen

## CI

Workflow `.github/workflows/checks.yml` chạy khi push, tạo pull request hoặc chạy thủ công:

- Python 3.12: cài dependency, chạy contract tests và xuất coverage XML.
- Java 21 + Android SDK 36: build debug, unit tests và Android Lint.
- Báo cáo được lưu trong artifacts của GitHub Actions. Không cần secret Supabase thật để chạy contract tests.

## SonarQube

Plugin SonarScanner for Gradle quét module Android và `backend/app`, nhận coverage Python từ `backend/coverage.xml`. Kết quả không thay thế kiểm thử RLS, database hoặc luồng trên thiết bị.

Workflow `.github/workflows/sonarqube.yml` chạy thủ công sau khi có:

- Repository Variables: `SONAR_HOST_URL`, `SONAR_PROJECT_KEY`.
- Repository Secret: `SONAR_TOKEN` với quyền phân tích project tương ứng.
- Máy chủ SonarQube phải truy cập được từ GitHub runner; địa chỉ localhost trên máy phát triển không đáp ứng điều kiện này.

Không dùng `pull_request_target` để chạy mã chưa tin cậy với token. Workflow quét chờ Quality Gate và báo lỗi nếu cấu hình thiếu hoặc gate không đạt.

## Chạy tại máy phát triển

Chọn JDK tương thích qua `JAVA_HOME` thay vì ghi đường dẫn máy vào `gradle.properties`.

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
cd backend
./.venv/Scripts/python.exe -m coverage run --source=app -m unittest discover -s tests -v
./.venv/Scripts/python.exe -m coverage xml
cd ..
./gradlew.bat sonar -Dsonar.qualitygate.wait=true
```

Trước khi quét, đặt `SONAR_HOST_URL`, `SONAR_PROJECT_KEY`, `SONAR_TOKEN` trong môi trường; không ghi token vào command hay file được Git theo dõi.

## Kết quả local ban đầu

Ngày 20/11/2026: Android build thành công, 10 unit tests đạt; backend 11 contract tests đạt, line coverage khoảng 52,23%. Android Lint còn 637 cảnh báo và không còn lỗi. Bốn lỗi WrongConstant được sửa bằng hằng Typeface. Các cảnh báo chủ yếu liên quan chuỗi giao diện, mô tả trợ năng, và tài nguyên chưa dùng; chưa tạo baseline để che cảnh báo.

## Quét SonarQube local ngày 20/11/2026

Đã khởi động container `andzen-sonarqube`, chỉ mở tại `http://127.0.0.1:9000`, dùng H2 cho đánh giá local. Không dùng cấu hình H2 này làm server production. Thông tin đăng nhập và token nằm trong `.sonarqube-local.json` được Git bỏ qua; không chia sẻ hoặc commit file này. Dashboard project: `http://127.0.0.1:9000/dashboard?id=AndZen`.

Quét đầu ghi nhận 10 vấn đề. Sau khi sửa thứ tự CORS middleware, tắt backup trong manifest chính và đơn giản hóa regex tách giờ, lần quét lại còn 6 vấn đề mở. Quality Gate trả `OK`, nhưng có `ignoredConditions=true`: điều kiện coverage được bỏ qua cho thay đổi nhỏ. Không diễn giải kết quả gate thành chứng nhận an toàn hoặc đủ coverage.

Các vấn đề còn lại cần đánh giá:

- Debug manifest: cảnh báo backup và HTTP cleartext. Manifest chính đã tắt backup và HTTP; debug vẫn dùng HTTP để kết nối API local. Cần kiểm tra manifest đã merge trước khi phát hành.
- `AuthSession`: Sonar nhận chuỗi tên header `apikey` là secret có thể hard-code. Giá trị thực tế là `BuildConfig.SUPABASE_PUBLISHABLE_KEY`; cần review và phân loại cảnh báo, không đổi tên header để né rule.
- `ApiClient`: hai cảnh báo regex cần rà về giới hạn đầu vào.
- `DateChipAdapter`: một biểu thức điều kiện trả cùng giá trị ở cả hai nhánh.

Coverage tổng Sonar lần đầu là 8,3%, khác với coverage backend riêng; Android chưa có báo cáo JaCoCo. Scanner còn cảnh báo unresolved types/imports và preview features, nên kết quả phân tích Java có thể chưa đầy đủ. Không tự động đóng các cảnh báo còn lại hoặc tắt rule.

GitHub runner không truy cập được SonarQube localhost này. Để quét trên GitHub, cần server riêng truy cập được từ runner và ba cấu hình đã nêu. CI build/test/lint hoạt động độc lập với server đó.
