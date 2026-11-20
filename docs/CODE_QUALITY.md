# Kiểm tra code AndZen

## CI

Workflow `.github/workflows/checks.yml` chạy khi push, tạo pull request hoặc chạy thủ công:

- Python 3.12: cài dependency, chạy contract tests và xuất coverage XML.
- Java 21 + Android SDK 37: build debug, unit tests và Android Lint.
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

## Xử lý 6 vấn đề còn lại — 20/11/2026

- Hai cảnh báo regex của `ApiClient`: thay regex xóa dấu slash cuối URL bằng vòng lặp tuyến tính trong `ApiUrlUtils`. Kiểm thử bảo toàn scheme/path và chuỗi 100.000 dấu slash đều đạt.
- `DateChipAdapter`: bỏ nhánh điều kiện có hai kết quả giống nhau; giữ nguyên màu đang hiển thị.
- Debug manifest: khai báo tắt backup và HTTP toàn cục. HTTP chỉ được phép qua Network Security Configuration trong source set debug cho `localhost`, `127.0.0.1`, `10.0.2.2`, `10.0.3.2`; không cho HTTP tới địa chỉ LAN tùy ý. Thiết bị USB dùng `adb reverse`, hoặc dùng HTTPS khi API ở máy khác.
- Kiểm tra manifest debug đã merge xác nhận `allowBackup=false`, `usesCleartextTraffic=false` và config mạng debug đúng. Manifest chính không tham chiếu ngoại lệ HTTP của debug.
- Cảnh báo `java:S6418` tại `AuthSession` được review: literal `apikey` là tên header, giá trị lấy từ cấu hình publishable key. Đã thêm giải thích và đánh dấu false positive cho đúng một issue trên SonarQube local; không tắt rule, không thêm `NOSONAR`. Trạng thái phân loại này nằm trong DB SonarQube local; máy chủ mới cần review lại issue tương ứng.

Sau các thay đổi: build debug, lint và 12 unit tests đạt; SonarQube còn 0 issue mở, Quality Gate `OK`. Android Lint vẫn có 638 cảnh báo, không có Error/Fatal; đây không phải tuyên bố code hết cảnh báo hay an toàn tuyệt đối. Các giới hạn coverage và unresolved types/imports đã nêu vẫn còn.

## Dọn toàn bộ Android Lint — kết quả mới nhất 20/11/2026

Đã xử lý toàn bộ 638 cảnh báo của báo cáo cũ. Lần chạy mới nhất có **0 Error/Fatal và 0 Warning**. Không dùng baseline, `tools:ignore`, `SuppressLint` hoặc tắt rule. Module Android bật `warningsAsErrors` và `abortOnError` để cảnh báo mới làm CI thất bại.

Các thay đổi chính:

- Đưa chuỗi XML và các lời gọi `setText` bị cảnh báo vào `strings_ui.xml`; dùng chuỗi định dạng thay cho nối chữ/số trực tiếp.
- Bổ sung mô tả cho nút có chức năng; ảnh trang trí được loại khỏi cây trợ năng. Thêm inputType/autofill theo ý nghĩa từng ô nhập, dùng Locale rõ ràng và API compat.
- Gộp icon/chữ thành compound drawables có kích thước tương ứng, giữ nút sao chép mã qua thao tác trên nhãn mã. Trang tài khoản giảm từ 132 xuống 79 view; divider dùng drawable thay cho view riêng. Các ô chọn khu vực/ngày sinh/giới tính dùng chính TextView làm nút chọn.
- Chuyển bitmap vào `drawable-nodpi`; bỏ tài nguyên được xác minh không còn tham chiếu; giữ icon Remix dùng bởi các drawable đã gộp.
- Thay refresh toàn bộ danh sách bằng DiffUtil và cập nhật đúng các hàng đổi trạng thái yêu thích/khung giờ đã chọn. Xử lý lại vị trí click của thông báo khi danh sách di chuyển.
- Thêm quy tắc loại trừ backup/cloud/device transfer. Chuyển nén/lưu ảnh sân khỏi UI thread; lỗi ghi ảnh được trả về biểu mẫu.
- Cập nhật các bản stable bị lint báo cũ, Gradle wrapper cùng checksum; compileSdk/targetSdk 37, minSdk 24. Giới hạn hai Gradle worker sau khi máy phát triển hết native memory trong một lượt chạy đồng thời emulator và SonarQube.

Kiểm chứng:

- Build debug + 12 unit tests đạt.
- Ba instrumentation tests đạt trên emulator Android 17: dựng và đo 26 layout với theme ứng dụng, kiểm tra các ô chọn hồ sơ sau khi làm phẳng layout, kiểm tra chuỗi định dạng, chính sách HTTP và DiffUtil không gửi global refresh.
- Đã mở và chụp màn hình đăng nhập của APK để kiểm tra việc render; không có crash AndroidRuntime được ghi nhận trong lần kiểm tra này.
- Không kiểm thử lại toàn bộ nghiệp vụ trên Supabase thật trong đợt dọn lint. Các giới hạn coverage và phân tích tĩnh đã nêu vẫn áp dụng. Cảnh báo deprecated từ toolchain Gradle/Java là nhóm riêng, không nằm trong số cảnh báo Android Lint đã dọn.

Khi debug bằng USB hoặc emulator, dùng `adb reverse tcp:8000 tcp:8000` với API `http://127.0.0.1:8000`. Nếu tự chuyển sang host thuộc mạng LAN khi target Android 17, phải tuân thủ quyền mạng local của Android; release tiếp tục yêu cầu HTTPS.

Lần quét SonarQube sau đợt dọn lint vẫn có **0 issue mở**, nhưng Quality Gate trả `ERROR` do coverage phần code mới chưa đạt ngưỡng 80%. Đợt sửa này không hạ ngưỡng hoặc loại code khỏi coverage để làm gate xanh. Android Lint, build/unit tests và kiểm tra layout trên emulator có kết quả riêng như trên; không diễn giải chúng thành coverage đã đủ hoặc toàn bộ nghiệp vụ đã được kiểm thử.
