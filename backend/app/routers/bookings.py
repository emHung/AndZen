import logging
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query

from app.booking_window import require_bookable_date
from app.schemas import BookingCreate
from app.security import CurrentUser, require_user
from app.supabase_client import get_supabase, read_query

router = APIRouter(tags=["bookings"])
logger = logging.getLogger(__name__)


@router.get("/bookings")
def list_bookings(user: CurrentUser = Depends(require_user)) -> list[dict]:
    result = read_query(
        get_supabase().table("bookings").select("*,pitches(*)").eq("user_id", user.id)
        .order("created_at", desc=True)
    )
    return result.data


@router.post("/bookings", status_code=201)
def create_booking(payload: BookingCreate, user: CurrentUser = Depends(require_user)) -> dict:
    require_bookable_date(payload.booking_date)
    client = get_supabase()
    slot_id = payload.slot_id
    try:
        result = client.rpc("create_booking", {
            "p_user_id": user.id,
            "p_pitch_id": payload.pitch_id,
            "p_slot_id": slot_id,
            "p_booking_date": payload.booking_date.isoformat(),
            "p_customer_name": payload.customer_name,
            "p_customer_phone": payload.customer_phone,
            "p_payment_method": payload.payment_method,
        }).execute()
    except Exception as exc:
        message = str(exc)
        if "slot_unavailable" in message or "unique" in message.lower():
            raise HTTPException(status_code=409, detail="Khung giờ vừa được đặt. Hãy chọn khung giờ khác.") from exc
        if "slot_not_found" in message or "pitch_not_found" in message:
            raise HTTPException(status_code=404, detail="Không tìm thấy sân hoặc khung giờ.") from exc
        if "booking_date_too_far" in message:
            raise HTTPException(status_code=422, detail="Chỉ có thể đặt sân trong 7 ngày tới.") from exc
        if "slot_in_past" in message or "booking_date_in_past" in message:
            raise HTTPException(status_code=422, detail="Khung giờ đã bắt đầu hoặc đã qua.") from exc
        raise HTTPException(status_code=502, detail="Không tạo được lịch đặt. Vui lòng thử lại.") from exc
    rows = result.data
    return rows[0] if isinstance(rows, list) else rows


@router.post("/bookings/{booking_id}/cancel")
def cancel_booking(booking_id: str, user: CurrentUser = Depends(require_user)) -> dict:
    try:
        result = get_supabase().rpc("cancel_booking", {"p_user_id": user.id, "p_booking_id": booking_id}).execute()
    except Exception as exc:
        if "booking_not_found" in str(exc):
            raise HTTPException(status_code=404, detail="Không tìm thấy lịch đặt.") from exc
        if "booking_not_cancellable" in str(exc):
            raise HTTPException(status_code=409, detail="Lịch này không thể hủy.") from exc
        raise HTTPException(status_code=502, detail="Không hủy được lịch đặt.") from exc
    rows = result.data
    return rows[0] if isinstance(rows, list) else rows


@router.post("/bookings/{booking_id}/reschedule")
def reschedule_booking(
    booking_id: str,
    slot_id: str = Query(min_length=1, max_length=160),
    booking_date: date = Query(),
    user: CurrentUser = Depends(require_user),
) -> dict:
    require_bookable_date(booking_date)
    try:
        client = get_supabase()
        existing = client.table("bookings").select("pitch_id").eq("id", booking_id).eq("user_id", user.id).maybe_single().execute().data
        if not existing:
            raise HTTPException(status_code=404, detail="Không tìm thấy lịch đặt.")
        result = client.rpc("reschedule_booking", {
            "p_user_id": user.id,
            "p_booking_id": booking_id,
            "p_slot_id": slot_id,
            "p_booking_date": booking_date.isoformat(),
        }).execute()
    except Exception as exc:
        if isinstance(exc, HTTPException):
            raise
        logger.exception("Booking reschedule failed", extra={"booking_id": booking_id, "user_id": user.id})
        if "slot_unavailable" in str(exc):
            raise HTTPException(status_code=409, detail="Khung giờ mới không còn trống.") from exc
        if "slot_not_found" in str(exc) or "booking_not_found" in str(exc) or "pitch_not_found" in str(exc):
            raise HTTPException(status_code=404, detail="Không tìm thấy lịch đặt hoặc khung giờ.") from exc
        if "slot_in_past" in str(exc) or "booking_not_cancellable" in str(exc):
            raise HTTPException(status_code=409, detail="Lịch đã bắt đầu hoặc đã qua; không thể đổi giờ.") from exc
        if "booking_date_in_past" in str(exc):
            raise HTTPException(status_code=422, detail="Không thể đổi lịch sang ngày đã qua.") from exc
        if "booking_date_too_far" in str(exc):
            raise HTTPException(status_code=422, detail="Chỉ có thể đổi lịch trong 7 ngày tới.") from exc
        raise HTTPException(status_code=502, detail="Không đổi được khung giờ.") from exc
    rows = result.data
    return rows[0] if isinstance(rows, list) else rows

