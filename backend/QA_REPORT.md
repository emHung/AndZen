# Kiểm tra hệ thống — 15/10/2026

## Phạm vi và kết quả

Android Java, FastAPI local và Supabase project `qafixjninsvalnsllawl`.

| Kiểm tra | Kết quả |
| --- | --- |
| Backend contract và xử lý lỗi đọc DB | 9/9 đạt |
| Android: nhập liệu, tiền tệ, thời gian Việt Nam, thời lượng | 10/10 đạt |
| Tích hợp API với DB thật | 31/31 đạt |
| Build APK debug | Thành công |
| UI trên Android emulator Small_Phone | Đặt, đổi ngày/giờ, thông báo cập nhật, hủy, số lịch sắp tới, lịch hoàn thành, bộ lọc giờ và quản lý nhân viên đã được thao tác |

Báo cáo tự động: `../build/qa-booking-report.json`. Không có điện thoại USB kết nối trong lượt kiểm tra cuối; kết quả UI là trên máy giả lập.

## Các lỗi đã sửa

- Đặt, hủy, đổi lịch và thao tác của quản lý cập nhật lịch/khung giờ trong cùng giao dịch DB; chống hai người đặt cùng giờ và chống tạo các khung giờ chồng nhau.
- Giới hạn đặt/đổi lịch từ hôm nay đến ngày thứ 7 tiếp theo, theo giờ Việt Nam. Khung giờ đã bắt đầu không được đặt, hủy hoặc đổi.
- Nhân viên chỉ xem và xử lý sân được phân công; quản trị viên có màn phân công sân.
- Email đăng nhập chỉ đổi qua luồng Supabase Auth xác nhận; số điện thoại liên hệ trong hồ sơ độc lập với định danh OTP. Hồ sơ không tự ghi email chưa xác nhận.
- Đồng bộ lần tải hồ sơ, trường trống và phiên đăng nhập; tránh xoay refresh token đồng thời.
- Sửa lỗi kết nối HTTP/2 trên Windows gây đọc DB chập chờn; dùng HTTP/1.1 với connection pool có giới hạn và retry có giới hạn cho truy vấn đọc. Không tự retry thao tác ghi.
- Bộ lọc lấy các khung giờ thật từ DB, lọc theo ngày/giờ kể cả khi chưa chọn ngày. Sửa crash khi mở màn lọc do lấy nhầm vùng cuộn, dùng vùng cuộn giờ có ID rõ ràng.
- Trạng thái ngày, danh sách giờ, số lịch sắp tới và tab được chọn khớp dữ liệu; lỗi tải không còn bị thể hiện như hết sân.
- Vé lấy ảnh, loại sân và tiện ích từ DB; lịch đã kết thúc ghi “Đã hoàn thành”, thời lượng lấy từ giờ thực tế.
- Kiểm tra đồng ý điều khoản trước khi gửi đặt sân; tải lại yêu thích khi quay về danh sách; sân thường đặt bỏ qua các lịch đã hủy.

## Dữ liệu thử nghiệm đã lưu

- 4 tài khoản: 2 khách hàng, 1 quản trị viên và 1 nhân viên thử nghiệm.
- 2 sân có nhãn `[TEST]`, 11 khung giờ/ngày; thêm tình huống ngày hết giờ trống và khung giờ đã qua.
- Mỗi tài khoản có lịch sắp tới, lịch hủy, lịch hoàn thành và sân yêu thích.
- Sau kiểm tra: 13 lịch được giữ lại, gồm 12 lịch mẫu và 1 lịch hủy từ thao tác UI. Lịch API tạm `[QA FLOW]` đã được dọn.
- Không thay mật khẩu hoặc thông tin cá nhân của tài khoản thật khi tạo dữ liệu mẫu. Thông tin hồ sơ/avatar/cài đặt tạm dùng cho QA đã khôi phục.

Kiểm tra DB sau QA: **0** lịch xác nhận thiếu khung giờ đã giữ, **0** khung giờ đã giữ thiếu lịch xác nhận, **0** khung giờ chồng nhau, **0** email hồ sơ lệch Auth, **0** lịch API tạm còn sót.

## Chạy lại

Từ thư mục `backend`:

```powershell
.\run-local.ps1
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
.\.venv\Scripts\python.exe qa_booking_flow.py          # chỉ xem kế hoạch
.\.venv\Scripts\python.exe qa_booking_flow.py --apply  # ghi dữ liệu TEST và chạy QA
```

API dùng cổng máy tính 8011; chuyển cổng Android 8000 về 8011 bằng ADB. Cổng máy tính 8000 thuộc ứng dụng khác. Chạy lại `run-local.ps1` khi thiết bị khởi động lại/mất kết nối để thiết lập lại chuyển cổng.

Từ thư mục dự án:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

APK: `../app/build/outputs/apk/debug/app-debug.apk`.

## Giới hạn còn lại

- SMS OTP đang tắt theo yêu cầu; đăng nhập/đăng ký dùng email. QR là lựa chọn ghi nhận phương thức thanh toán, chưa tạo giao dịch hay xác nhận tiền tự động.
- Thông báo trong app hiện được dựng từ trạng thái lịch, chưa có lịch sử sự kiện độc lập hoặc push notification.
- FastAPI chạy local, cần máy tính và kết nối tới Supabase để dùng app.
- Supabase còn cảnh báo tính năng chặn mật khẩu đã rò rỉ chưa bật: [hướng dẫn](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection). Không tự nâng gói dịch vụ.
- Hai thông tin RLS không có policy ở `bookings` và `staff_pitch_assignments` là chủ đích: không cho client truy cập trực tiếp; FastAPI kiểm tra quyền và dùng service role.
- Build còn cảnh báo công cụ JDK/native access, SDK XML khác phiên bản và một số Android API cũ. Build và kiểm tra đều đạt; nâng bộ công cụ cần được thực hiện riêng.

Ảnh bằng chứng nằm trong `../build/qa/`: `native-success.png`, `native-rescheduled-notification.png`, `native-cancelled.png`, `native-completed.png`, `native-filter.png`, `native-admin-staff.png`.
