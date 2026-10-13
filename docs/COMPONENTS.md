# Đặc tả Thành phần Ứng dụng (Components Specification)

## 1. Activities & Controllers (Quy tắc $\le 150$ dòng code)

| Activity / Controller | Mục đích & Trách nhiệm chính |
|---|---|
| `AuthActivity` | Quản lý màn hình Đăng nhập / Đăng ký, chuyển đổi tab, validate form và chuyển hướng vào `MainActivity`. |
| `MainActivity` | Quản lý BottomNavigationView điều hướng 4 Fragment chính (`PitchList`, `Favorites`, `MyBookings`, `Profile`). |
| `PitchFilterActivity` | Nhận và trả về kết quả bộ lọc nâng cao (loại sân, giá, tiện ích) qua Intent result. |
| `PitchDetailActivity` | Hiển thị thông tin sân, 4 hình ảnh sân sạch, toggle yêu thích, dải chọn ngày có slot trống và danh sách khung giờ chưa đặt. |
| `BookingConfirmActivity` | Tóm tắt đơn hàng, tiếp nhận thông tin người đặt và xác nhận tạo booking qua `BookingRepository`. |
| `BookingSuccessActivity` | Hiển thị kết quả đặt sân thành công với mã đặt và nút điều hướng tới lịch đặt hoặc trang chủ. |
| `BookingDetailActivity` | Chi tiết lịch đặt, điều phối mở dialog xác nhận hủy lịch hoặc dialog đổi khung giờ chơi. |
| `EditProfileActivity` | Chỉnh sửa họ tên, SĐT, email, địa chỉ và ngày sinh (sử dụng Material DatePicker tùy biến sáng sủa). |
| `NotificationActivity` | Danh sách thông báo hệ thống và cập nhật trạng thái lịch đặt sân. |

## 2. Fragments

- **`PitchListFragment`**: Tìm kiếm từ khóa, chip phân loại sân 5/7/11, danh sách RecyclerView sân bóng.
- **`FavoritesFragment`**: Danh sách sân yêu thích, hỗ trợ bỏ yêu thích trực tiếp trên thẻ.
- **`MyBookingsFragment`**: 3 tab trạng thái (Sắp tới, Đã hoàn thành, Đã hủy) kèm RecyclerView danh sách lịch đặt.
- **`ProfileFragment`**: Thống kê số trận đá, sân thích, điều hướng chỉnh sửa hồ sơ và đăng xuất.

## 3. Adapters

- **`PitchAdapter`**: Hiển thị thẻ sân bóng trong danh sách tìm kiếm (`item_pitch_card.xml`).
- **`FavoritePitchAdapter`**: Hiển thị thẻ sân bóng yêu thích (`item_favorite_pitch.xml`).
- **`DateChipAdapter`**: Hiển thị từng chip ngày trong tuần (`item_date_chip.xml`), tự động làm mờ hoặc ẩn các ngày không có khung giờ nào còn trống.
- **`TimeSlotAdapter`**: Hiển thị danh sách khung giờ (`item_time_slot.xml`), hỗ trợ chọn 1 slot tại một thời điểm, chỉ nạp các slot chưa đặt (`SlotStatus.AVAILABLE`).
- **`BookingAdapter`**: Hiển thị thẻ lịch đặt (`item_booking_card.xml`) với badge trạng thái màu sắc rõ ràng.
- **`NotificationAdapter`**: Hiển thị thẻ thông báo (`item_notification.xml`) với icon danh mục tương ứng.

## 4. Dialogs

- **`DialogCancelBooking` (`dialog_cancel_booking.xml`)**: Dialog xác nhận hủy sân với các lựa chọn lý do (Bận việc đột xuất, Thay đổi thời gian, Thời tiết không thuận lợi, Khác).
- **`DialogRescheduleSlot` (`dialog_reschedule_slot.xml`)**: Dialog đổi lịch đặt sân, cho phép chọn ngày mới và slot giờ trống mới trực tiếp trên popup.

## 5. Dữ liệu & Tiện ích (Data & Utilities)

- **`BookingRepository`**: Singleton quản lý trạng thái dữ liệu (Sân, Slot, Booking) xuyên suốt vòng đời ứng dụng.
- **`MockPitchData` / `MockSlotData`**: Cung cấp dữ liệu mẫu phong phú cho các sân bóng tại Hà Nội kèm các khung giờ đa dạng.
- **`CurrencyUtils`**: Định dạng tiền tệ VND chuẩn Việt Nam (`###,### đ`).
- **`DateTimeUtils`**: Định dạng ngày, thứ trong tuần tiếng Việt (`Thứ Hai`, `Thứ Ba`, v.v.) và format giờ bắt đầu - kết thúc.
- **`ValidationUtils`**: Hàm kiểm tra số điện thoại Việt Nam (10 số, đầu số hợp lệ) và kiểm tra định dạng email chuẩn.
- **`IdGenerator`**: Sinh mã booking ngẫu nhiên định dạng `SANBONG-XXXXXX`.

## 6. Hệ thống Icon Remix Icons (Vector Drawables)

Toàn bộ icon ứng dụng được chuẩn hóa 100% theo vector drawable của bộ **Remix Icons**:
- `ic_ri_heart`, `ic_ri_heart_fill`: Yêu thích / Bỏ thích.
- `ic_ri_calendar`: Lịch, ngày tháng.
- `ic_ri_time`: Đồng hồ, khung giờ.
- `ic_ri_map_pin`: Vị trí, địa chỉ sân.
- `ic_ri_bolt`: Đèn LED chiếu sáng.
- `ic_ri_car`: Bãi gửi xe máy & ô tô.
- `ic_ri_shirt`: Phòng thay đồ, áo bib.
- `ic_ri_drop`: Nước uống miễn phí.
- `ic_ri_football`: Quả bóng đá, biểu tượng sân.
- `ic_ri_edit`, `ic_ri_camera`, `ic_ri_delete_bin`: Thao tác người dùng.
