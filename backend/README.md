# Sân Bóng API

FastAPI backend cho ứng dụng Android. Supabase Auth quản lý danh tính, Postgres giữ hồ sơ/lịch/sân, Storage giữ ảnh đại diện. QR hiện chỉ là lựa chọn thanh toán và nhãn hướng dẫn; không tạo giao dịch hoặc xác nhận tiền.

## Chạy local

1. Cài Python 3.11+.
2. Tạo môi trường ảo và cài dependency:

   ```powershell
   py -m venv .venv
   .\.venv\Scripts\Activate.ps1
   pip install -r requirements.txt
   Copy-Item .env.example .env
   ```

3. Tạo Supabase project mới. Lấy Project URL, Publishable key và Secret key trong Dashboard. Điền URL, Secret key vào `backend/.env`; không đưa Secret key vào app hoặc Git.
4. Chạy toàn bộ migrations trong `supabase/migrations` theo thứ tự tên file. Chỉ chạy migration đầu sẽ thiếu ràng buộc lịch và phân quyền nhân viên.
5. MVP dùng email/mật khẩu. OTP SMS mặc định ẩn; chỉ bật `sanbong.phoneOtpEnabled=true` sau khi có SMS provider. Google OAuth cần provider và redirect URL `com.sanbong.app://auth-callback`.
6. Chạy API từ thư mục `backend`:

   ```powershell
   .\run-local.ps1
   ```

   Health: `http://127.0.0.1:8011/health`; OpenAPI: `http://127.0.0.1:8011/docs`. Script kiểm tra đúng dịch vụ và chuyển tiếp cổng Android 8000 tới API 8011. Giữ cửa sổ chạy API mở khi dùng app.

## Kết nối Android trên máy thật qua USB

Trong `local.properties` ở thư mục repo Android (file này đã bị Git bỏ qua), thêm:

```properties
sanbong.supabaseUrl=https://YOUR_PROJECT.supabase.co
sanbong.supabasePublishableKey=sb_publishable_...
sanbong.apiBaseUrl=http://127.0.0.1:8000
```

Chạy `run-local.ps1`, hoặc khởi động API trên 8011 rồi chạy `adb reverse tcp:8000 tcp:8011`. Có nhiều thiết bị thì dùng `run-local.ps1 -Serial DEVICE_ID`. Bản release cần URL HTTPS.

## Tạo tài khoản nhân viên/quản trị

Đăng ký tài khoản trước, sau đó gán quyền trong SQL Editor bằng UUID người dùng từ Supabase Auth:

```sql
update public.profiles set role = 'admin' where id = 'USER_UUID';
-- Hoặc role = 'staff' cho nhân viên sân.
```

API chỉ cho phép role `staff`/`admin` thay đổi dữ liệu quản lý. Khi API dùng Secret key, key này vượt qua RLS; vì vậy backend luôn xác thực JWT, kiểm tra role và tự giới hạn truy vấn theo user.

## Quản trị trên ứng dụng Android

Đăng nhập bằng tài khoản có role `staff` hoặc `admin`, mở tab Tài khoản rồi chọn Quản lý sân. Màn hình Android gọi các API `/v1/admin/*` của FastAPI; backend xác thực JWT và kiểm tra role trước khi đọc/ghi Supabase. Nhân viên quản lý lịch đặt, trạng thái thu tiền và khung giờ. Chỉ admin được thêm/sửa thông tin sân.

Danh sách sân, lịch và khung giờ lấy từ DB, không tự sinh khi mở màn hình. Admin tạo sân; admin/nhân viên được phân công tạo khung giờ trong 7 ngày tới. DB chặn khung giờ chồng lấn. Đặt, đổi và hủy lịch cập nhật lịch/khung giờ trong một giao dịch.

Tab Nhân viên của admin cho phép phân công sân cho tài khoản có role `staff`. Nhân viên chưa được phân công không thấy lịch/sân của hệ thống.

Số điện thoại hồ sơ là số liên hệ đặt sân; thay đổi số này không đổi danh tính OTP. Đổi email dùng xác nhận Supabase Auth; email hồ sơ chỉ đồng bộ sau khi Auth đổi thành công.

## Lưu ý cấu hình provider

- Phone OTP cần cấu hình SMS provider và hạn mức gửi ở Supabase Auth.
- Google cần OAuth Client ID/Secret ở Google Cloud và cấu hình provider tương ứng trong Supabase.
- Email/password cần thiết lập email SMTP/confirmation phù hợp trong Supabase Auth.
- Tài khoản Supabase chưa có trong repo; các luồng xác thực và ghi dữ liệu thật chỉ chạy sau khi điền các giá trị trên.

