# Triển khai JWT/RLS

## Trạng thái

- `ZenQProJect` là project AndZen đang sử dụng.
- Migration `20261019150000_customer_jwt_rls.sql` mới được chuẩn bị, chưa áp dụng lên project này.
- Chưa có Supabase staging: tạo project mới bị chặn bởi quota Free; branching cần gói phù hợp.
- Kiểm tra contract local không thay thế kiểm tra RLS thực tế trên database.

## Thứ tự triển khai

1. Tạo project staging riêng và cấu hình FastAPI trỏ tới project đó.
2. Áp dụng các migration trong `backend/supabase/migrations` theo thứ tự.
3. Dùng tài khoản thử customer A, customer B, staff và admin. Kiểm tra đọc/sửa chéo hồ sơ, settings, avatar, favorites và bookings bị chặn; customer không tự đổi role hay gọi API admin được.
4. Gọi trực tiếp Data API/RPC bằng JWT customer: giả `p_user_id` phải bị từ chối; đặt, đổi ngày/giờ, hủy và tranh chấp cùng slot phải giữ dữ liệu nhất quán.
5. Kiểm tra staff chỉ quản lý sân được phân công; kiểm tra admin và luồng guest đọc sân/khung giờ.
6. Sau khi các bước staging đạt, triển khai migration rồi backend tương ứng. Backend mới cần policy và quyền execute mới; chạy backend mới với DB chưa migration có thể làm lỗi các luồng khách hàng.

## Quyền backend còn lại

Client đặc quyền vẫn phục vụ các thao tác quản trị, xóa tài khoản, tra cứu email Auth và health check. Các thao tác khách hàng dùng publishable key cùng JWT cá nhân. Khóa bí mật phải nằm ở cấu hình backend và không nằm trong APK.
