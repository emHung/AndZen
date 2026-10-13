package com.sanbong.app.data;

import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.SlotStatus;
import com.sanbong.app.model.TimeSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kho dữ liệu tập trung lưu trữ trạng thái Sân, Khung giờ và Lịch đặt
 */
public class BookingRepository {

    private static BookingRepository instance;

    private final List<Pitch> pitches;
    private final Map<String, List<TimeSlot>> slotsMap; // Key: pitchId_date
    private final List<Booking> myBookings;
    private final java.util.Set<String> favoritePitchIds;
    private final Booking sampleBooking;
    private final boolean includeSampleBooking;

    public BookingRepository() {
        this(true);
    }

    public BookingRepository(boolean includeSampleBooking) {
        this.includeSampleBooking = includeSampleBooking;
        this.pitches = new ArrayList<>(MockPitchData.createSamplePitches());
        this.slotsMap = new HashMap<>();
        this.myBookings = new ArrayList<>();
        this.favoritePitchIds = new java.util.HashSet<>(java.util.Arrays.asList("p1", "p2", "p3"));
        String today = com.sanbong.app.util.DateTimeUtils.getNextDays(1).get(0);
        sampleBooking = new Booking(
                "SB-X8U4EE", "p1", "Sân bóng Thượng Đình",
                "129 Nguyễn Trãi, Thanh Xuân, Hà Nội", "p1_" + today + "_3", today,
                "16:30 - 18:00", 350000, "Nguyễn Văn A", "036 241 5016"
        );
        if (includeSampleBooking) this.myBookings.add(sampleBooking);
    }

    public static synchronized BookingRepository getInstance() {
        if (instance == null) {
            instance = new BookingRepository();
        }
        return instance;
    }

    public synchronized void resetForTests() {
        slotsMap.clear();
        myBookings.clear();
        if (includeSampleBooking) myBookings.add(sampleBooking);
        sampleBooking.setStatus(BookingStatus.CONFIRMED);
        sampleBooking.setSlotId("p1_" + sampleBooking.getDate() + "_3");
        sampleBooking.setTimeRange("16:30 - 18:00");
        favoritePitchIds.clear();
        favoritePitchIds.addAll(java.util.Arrays.asList("p1", "p2", "p3"));
    }

    public List<Pitch> getAllPitches() {
        return new ArrayList<>(pitches);
    }

    public Pitch getPitchById(String pitchId) {
        for (Pitch p : pitches) {
            if (p.getId().equalsIgnoreCase(pitchId)) {
                return p;
            }
        }
        return null;
    }

    public Booking getBookingByCode(String code) {
        for (Booking b : myBookings) {
            if (b.getBookingCode().equalsIgnoreCase(code)) {
                return b;
            }
        }
        return null;
    }

    public synchronized List<TimeSlot> getSlots(String pitchId, String date) {
        String key = pitchId + "_" + date;
        if (!slotsMap.containsKey(key)) {
            Pitch pitch = getPitchById(pitchId);
            long basePrice = (pitch != null) ? pitch.getMinPrice() : 300000;
            slotsMap.put(key, MockSlotData.generateSlots(pitchId, date, basePrice));
        }
        List<TimeSlot> slots = slotsMap.get(key);
        // Booking records are the source of truth, so regenerated slot lists
        // cannot accidentally show a confirmed booking as available.
        for (Booking booking : myBookings) {
            if (booking.getStatus() != BookingStatus.CONFIRMED
                    || !booking.getPitchId().equals(pitchId)
                    || !booking.getDate().equals(date)) continue;
            TimeSlot bookedSlot = findSlot(slots, booking.getSlotId());
            if (bookedSlot != null) bookedSlot.setStatus(SlotStatus.BOOKED);
        }
        return slots;
    }

    public synchronized boolean confirmBooking(Booking booking) {
        if (booking == null || booking.getSlotId() == null) return false;
        // Cập nhật trạng thái slot sang BOOKED
        List<TimeSlot> slots = getSlots(booking.getPitchId(), booking.getDate());
        if (slots != null) {
            for (TimeSlot slot : slots) {
                if (slot.getId().equals(booking.getSlotId())) {
                    if (!slot.isAvailable()) {
                        return false; // Đã có người đặt trước
                    }
                    slot.setStatus(SlotStatus.BOOKED);
                    myBookings.add(0, booking);
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized boolean cancelBooking(String bookingCode) {
        if (bookingCode == null) return false;
        for (Booking booking : myBookings) {
            if (booking.getBookingCode().equalsIgnoreCase(bookingCode)
                    && booking.getStatus() == BookingStatus.CONFIRMED) {
                List<TimeSlot> slots = getSlots(booking.getPitchId(), booking.getDate());
                TimeSlot bookedSlot = findSlot(slots, booking.getSlotId());
                booking.setStatus(BookingStatus.CANCELLED);
                if (bookedSlot != null && bookedSlot.getStatus() == SlotStatus.BOOKED
                        && !isSlotBookedByAnother(bookedSlot, booking)) {
                    bookedSlot.setStatus(SlotStatus.AVAILABLE);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized boolean rescheduleBooking(String code, String newSlotId) {
        if (code == null || newSlotId == null) return false;
        for (Booking b : myBookings) {
            if (b.getBookingCode().equalsIgnoreCase(code) && b.getStatus() == BookingStatus.CONFIRMED) {
                List<TimeSlot> slots = getSlots(b.getPitchId(), b.getDate());
                TimeSlot oldSlot = findSlot(slots, b.getSlotId());
                TimeSlot newSlot = findSlot(slots, newSlotId);
                if (newSlot == null || newSlot.getStatus() == SlotStatus.UNAVAILABLE
                        || (newSlot.getStatus() == SlotStatus.BOOKED && newSlot != oldSlot)) return false;
                if (oldSlot != null && oldSlot != newSlot && oldSlot.getStatus() == SlotStatus.BOOKED) {
                    boolean bookedByAnother = isSlotBookedByAnother(oldSlot, b);
                    if (!bookedByAnother) oldSlot.setStatus(SlotStatus.AVAILABLE);
                }
                newSlot.setStatus(SlotStatus.BOOKED);
                b.setTimeRange(newSlot.getTimeRange());
                b.setSlotId(newSlot.getId());
                return true;
            }
        }
        return false;
    }

    private TimeSlot findSlot(List<TimeSlot> slots, String id) {
        if (slots == null || id == null) return null;
        for (TimeSlot slot : slots) if (slot.getId().equals(id)) return slot;
        return null;
    }

    private boolean isSlotBookedByAnother(TimeSlot slot, Booking excluded) {
        for (Booking booking : myBookings) {
            if (booking != excluded && booking.getStatus() == BookingStatus.CONFIRMED
                    && booking.getPitchId().equals(slot.getPitchId())
                    && booking.getDate().equals(slot.getDate())
                    && booking.getSlotId().equals(slot.getId())) return true;
        }
        return false;
    }

    public List<Booking> getMyBookings() {
        return new ArrayList<>(myBookings);
    }

    public synchronized boolean isFavorite(String pitchId) {
        return favoritePitchIds.contains(pitchId.toLowerCase());
    }

    public synchronized boolean toggleFavorite(String pitchId) {
        String key = pitchId.toLowerCase();
        if (favoritePitchIds.contains(key)) {
            favoritePitchIds.remove(key);
            return false;
        } else {
            favoritePitchIds.add(key);
            return true;
        }
    }

    public synchronized List<Pitch> getFavoritePitches() {
        List<Pitch> list = new ArrayList<>();
        for (Pitch p : pitches) {
            if (favoritePitchIds.contains(p.getId().toLowerCase())) {
                list.add(p);
            }
        }
        return list;
    }

    public synchronized int getFavoriteCount() {
        return favoritePitchIds.size();
    }

    public int getAvailableSlotsCount(String pitchId, String date) {
        List<TimeSlot> slots = getSlots(pitchId, date);
        int count = 0;
        if (slots != null) {
            for (TimeSlot s : slots) {
                if (s.isAvailable()) count++;
            }
        }
        return count;
    }

    public String getAvailabilityText(String pitchId, String date) {
        int count = getAvailableSlotsCount(pitchId, date);
        return count > 0 ? "Còn " + count + " khung giờ" : "Hết sân hôm nay";
    }

    public List<String> getAvailableDates(String pitchId, int days) {
        List<String> list = new ArrayList<>();
        List<String> nextDays = com.sanbong.app.util.DateTimeUtils.getNextDays(days);
        for (String d : nextDays) {
            if (getAvailableSlotsCount(pitchId, d) > 0) {
                list.add(d);
            }
        }
        return list;
    }

    public List<TimeSlot> getAvailableSlots(String pitchId, String date) {
        List<TimeSlot> result = new ArrayList<>();
        List<TimeSlot> all = getSlots(pitchId, date);
        if (all != null) {
            for (TimeSlot s : all) {
                if (s.isAvailable()) result.add(s);
            }
        }
        return result;
    }

    public String getFirstAvailableDate(String pitchId, String startDate) {
        List<String> days = com.sanbong.app.util.DateTimeUtils.getNextDays(14);
        boolean started = false;
        for (String d : days) {
            if (d.equals(startDate)) started = true;
            if (started && getAvailableSlotsCount(pitchId, d) > 0) return d;
        }
        for (String d : days) {
            if (getAvailableSlotsCount(pitchId, d) > 0) return d;
        }
        return days.get(0);
    }
}


