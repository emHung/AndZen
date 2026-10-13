package com.sanbong.app;

import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.SlotStatus;
import com.sanbong.app.model.TimeSlot;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Kiểm thử toàn bộ nghiệp vụ đặt và hủy sân trong BookingRepository
 */
public class BookingRepositoryTest {

    private BookingRepository repository;

    @Before
    public void setUp() {
        repository = new BookingRepository(false);
    }

    @Test
    public void testPitchesCoverage() {
        List<Pitch> pitches = repository.getAllPitches();
        assertNotNull(pitches);
        assertTrue("Dữ liệu mẫu phải có ít nhất 6 sân", pitches.size() >= 6);
    }

    @Test
    public void testBookingFlowAndRevertOnCancel() {
        String pitchId = "p1";
        String date = "2026-10-15";
        List<TimeSlot> slots = repository.getSlots(pitchId, date);
        assertNotNull(slots);
        assertFalse(slots.isEmpty());

        // Tìm một slot khả dụng
        TimeSlot targetSlot = null;
        for (TimeSlot s : slots) {
            if (s.isAvailable()) {
                targetSlot = s;
                break;
            }
        }
        assertNotNull("Phải có ít nhất 1 slot còn trống để test", targetSlot);

        // 1. Thực hiện đặt sân
        Booking booking = new Booking(
                "SB-TEST01",
                pitchId,
                "Sân bóng Thượng Đình",
                "129 Nguyễn Trãi, Thanh Xuân, Hà Nội",
                targetSlot.getId(),
                date,
                targetSlot.getTimeRange(),
                targetSlot.getPrice(),
                "Nguyễn Văn Test",
                "0912345678"
        );

        boolean success = repository.confirmBooking(booking);
        assertTrue("Đặt sân với slot trống phải thành công", success);
        assertEquals(SlotStatus.BOOKED, targetSlot.getStatus());

        // 2. Không cho phép đặt trùng cùng 1 slot
        boolean duplicateSuccess = repository.confirmBooking(booking);
        assertFalse("Đặt trùng slot đã BOOKED phải thất bại", duplicateSuccess);

        // 3. Hủy đặt sân
        boolean cancelSuccess = repository.cancelBooking("SB-TEST01");
        assertTrue("Hủy đặt sân phải thành công", cancelSuccess);
        assertEquals("Hủy sân phải chuyển slot về AVAILABLE", SlotStatus.AVAILABLE, targetSlot.getStatus());
    }

    @Test
    public void testRescheduleMovesBookingAndReleasesOldSlot() {
        String pitchId = "p1";
        String date = "2026-10-15";
        List<TimeSlot> slots = repository.getSlots(pitchId, date);
        TimeSlot oldSlot = slots.get(2);
        TimeSlot newSlot = slots.get(4);
        Booking booking = new Booking("SB-MOVE01", pitchId, "Test pitch", "Test address",
                oldSlot.getId(), date, oldSlot.getTimeRange(), oldSlot.getPrice(), "Test User", "0912345678");

        assertTrue(repository.confirmBooking(booking));
        assertTrue(repository.rescheduleBooking("SB-MOVE01", newSlot.getId()));
        assertEquals(SlotStatus.AVAILABLE, oldSlot.getStatus());
        assertEquals(SlotStatus.BOOKED, newSlot.getStatus());
        assertEquals(newSlot.getId(), booking.getSlotId());
        assertEquals(newSlot.getTimeRange(), booking.getTimeRange());
    }

    @Test
    public void testRescheduleRejectsBookedSlotAndCancelIsIdempotent() {
        String pitchId = "p1";
        String date = "2026-10-15";
        List<TimeSlot> slots = repository.getSlots(pitchId, date);
        TimeSlot available = slots.get(1);
        TimeSlot booked = null;
        for (TimeSlot slot : slots) {
            if (slot.getStatus() == SlotStatus.BOOKED) { booked = slot; break; }
        }
        assertNotNull(booked);
        Booking booking = new Booking("SB-MOVE02", pitchId, "Test pitch", "Test address",
                available.getId(), date, available.getTimeRange(), available.getPrice(), "Test User", "0912345678");

        assertTrue(repository.confirmBooking(booking));
        assertFalse(repository.rescheduleBooking("SB-MOVE02", booked.getId()));
        assertEquals(SlotStatus.BOOKED, available.getStatus());
        assertEquals(available.getId(), booking.getSlotId());
        assertTrue(repository.cancelBooking("SB-MOVE02"));
        assertFalse(repository.cancelBooking("SB-MOVE02"));
        assertEquals(SlotStatus.AVAILABLE, available.getStatus());
    }
}





