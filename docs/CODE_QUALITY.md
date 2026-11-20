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
