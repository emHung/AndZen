package com.sanbong.app.data;

import com.sanbong.app.R;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.PitchType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Nguồn dữ liệu mẫu cho các sân bóng (kèm hình ảnh thực tế)
 */
public final class MockPitchData {

    private MockPitchData() {}

    public static List<Pitch> createSamplePitches() {
        List<Pitch> pitches = new ArrayList<>();

        pitches.add(new Pitch(
                "p1",
                "Sân bóng Thượng Đình",
                "Thanh Xuân",
                "129 Nguyễn Trãi, Thanh Xuân, Hà Nội",
                PitchType.SEVEN,
                350000,
                4.8,
                Arrays.asList("Sân 7 người", "Đèn LED", "Nước miễn phí", "Bãi xe rộng"),
                "Mặt cỏ nhân tạo mới thay năm 2024, hệ thống thoát nước tốt, đèn sáng chuẩn thi đấu.",
                R.drawable.img_pitch_1
        ));

        pitches.add(new Pitch(
                "p2",
                "Sân bóng Cầu Giấy Arena",
                "Cầu Giấy",
                "35 Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
                PitchType.SEVEN,
                400000,
                4.7,
                Arrays.asList("Sân 7 người", "Có mái che", "Phòng thay đồ", "Bảo vệ 24/7"),
                "Khu phức hợp thể thao hiện đại, phục vụ căng tin và nước giải khát đầy đủ.",
                R.drawable.img_pitch_2
        ));

        pitches.add(new Pitch(
                "p3",
                "Sân mini Mỹ Đình Stars",
                "Nam Từ Liêm",
                "Lê Đức Thọ, Mỹ Đình, Nam Từ Liêm, Hà Nội",
                PitchType.FIVE,
                250000,
                4.6,
                Arrays.asList("Sân 5 người", "Đèn cao áp", "Gửi xe máy ô tô", "Áo pitch tập"),
                "Chuyên sân 5 người chất lượng cao, cỏ êm chân, phù hợp đá giao lưu sau giờ làm.",
                R.drawable.img_pitch_sunset
        ));

        pitches.add(new Pitch(
                "p4",
                "Sân vận động Trung Kính",
                "Cầu Giấy",
                "215 Trung Kính, Cầu Giấy, Hà Nội",
                PitchType.FIVE,
                280000,
                4.5,
                Arrays.asList("Sân 5 người", "Đèn LED", "Tủ đồ cá nhân", "Căng tin"),
                "Vị trí trung tâm thuận tiện di chuyển, sân cỏ được bảo dưỡng định kỳ hàng tháng.",
                R.drawable.img_pitch_night
        ));

        pitches.add(new Pitch(
                "p5",
                "Sân bóng Tân Triều Park",
                "Thanh Xuân",
                "Ngõ 300 Nguyễn Xiển, Thanh Xuân, Hà Nội",
                PitchType.ELEVEN,
                900000,
                4.9,
                Arrays.asList("Sân 11 người", "Khán đài", "Phòng tắm nóng lạnh", "Bãi xe"),
                "Sân bóng 11 người đạt tiêu chuẩn giải đấu phong trào, mặt sân rộng rãi bằng phẳng.",
                R.drawable.img_pitch_1
        ));

        pitches.add(new Pitch(
                "p6",
                "Sân bóng Keangnam Garden",
                "Nam Từ Liêm",
                "Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội",
                PitchType.SEVEN,
                420000,
                4.8,
                Arrays.asList("Sân 7 người", "Đèn LED", "Cho thuê giày", "Wifi miễn phí"),
                "Không gian thoáng mát cạnh tòa Keangnam, hệ thống lưới chắn và cỏ mềm an toàn.",
                R.drawable.img_pitch_2
        ));

        return Collections.unmodifiableList(pitches);
    }
}
