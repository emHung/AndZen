import logging
from datetime import date, time
from uuid import UUID, uuid4

from fastapi import APIRouter, Depends, HTTPException, Query

from app.booking_window import require_bookable_date
from app.management import managed_pitch_ids, mutation_error, require_pitch_access
from app.schemas import (BookingStatusUpdate, PaymentStatusUpdate, PitchCreate, PitchUpdate,
                         SlotCreate, SlotStatusUpdate, StaffAssignments)
from app.security import CurrentUser, require_admin, require_staff
from app.supabase_client import get_supabase

router = APIRouter(prefix="/admin", tags=["admin"])
logger = logging.getLogger(__name__)


def run_mutation(function: str, values: dict) -> dict:
    try:
        rows = get_supabase().rpc(function, values).execute().data
        return rows[0] if isinstance(rows, list) and rows else rows
    except Exception as exc:
        error = mutation_error(exc)
        if error.status_code >= 500:
            logger.error("Management operation failed (%s)", type(exc).__name__, extra={"operation": function})
        else:
            logger.info("Management operation rejected", extra={"operation": function, "status": error.status_code})
        raise error from exc


@router.get("/bookings")
def list_all_bookings(user: CurrentUser = Depends(require_staff)) -> list[dict]:
    allowed = managed_pitch_ids(user)
    if allowed == []:
        return []
    query = get_supabase().table("bookings").select("*")
    if allowed is not None:
        query = query.in_("pitch_id", allowed)
    return query.order("booking_date", desc=True).execute().data


@router.get("/pitches")
def list_admin_pitches(user: CurrentUser = Depends(require_staff)) -> list[dict]:
    allowed = managed_pitch_ids(user)
    if allowed == []:
        return []
    query = get_supabase().table("pitches").select("*")
    if allowed is not None:
        query = query.in_("id", allowed)
    return query.order("id").execute().data


@router.get("/staff")
def list_staff(user: CurrentUser = Depends(require_admin)) -> list[dict]:
    client = get_supabase()
    people = client.table("profiles").select("id,full_name,email").eq("role", "staff").execute().data
    assignments = client.table("staff_pitch_assignments").select("*").execute().data
    for person in people:
        person["pitch_ids"] = [row["pitch_id"] for row in assignments if row["user_id"] == person["id"]]
    return people


@router.put("/staff/{staff_id}/pitches")
def assign_staff(staff_id: UUID, payload: StaffAssignments, user: CurrentUser = Depends(require_admin)) -> dict:
    run_mutation("assign_staff_pitches", {"p_actor_id": user.id, "p_staff_id": str(staff_id), "p_pitch_ids": payload.pitch_ids})
    return {"pitch_ids": payload.pitch_ids}


@router.patch("/bookings/{booking_id}/payment")
def set_payment_status(booking_id: UUID, payload: PaymentStatusUpdate, user: CurrentUser = Depends(require_staff)) -> dict:
    return run_mutation("admin_booking_payment", {"p_actor_id": user.id, "p_booking_id": str(booking_id),
                                                "p_payment_status": payload.payment_status})


@router.patch("/bookings/{booking_id}")
def update_booking_status(booking_id: UUID, payload: BookingStatusUpdate, user: CurrentUser = Depends(require_staff)) -> dict:
    return run_mutation("admin_booking_status", {"p_actor_id": user.id, "p_booking_id": str(booking_id), "p_status": payload.status})


@router.post("/pitches", status_code=201)
def create_pitch(payload: PitchCreate, user: CurrentUser = Depends(require_admin)) -> dict:
    try:
        return get_supabase().table("pitches").insert(payload.model_dump()).execute().data[0]
    except Exception as exc:
        if "23505" in str(exc):
            raise HTTPException(status_code=409, detail="Mã sân đã tồn tại.") from exc
        raise mutation_error(exc) from exc


@router.patch("/pitches/{pitch_id}")
def update_pitch(pitch_id: str, payload: PitchUpdate, user: CurrentUser = Depends(require_admin)) -> dict:
    result = get_supabase().table("pitches").update(payload.model_dump()).eq("id", pitch_id).execute()
    if not result.data:
        raise HTTPException(status_code=404, detail="Không tìm thấy sân.")
    return result.data[0]


@router.post("/pitches/{pitch_id}/slots", status_code=201)
def create_slot(pitch_id: str, payload: SlotCreate, user: CurrentUser = Depends(require_staff)) -> dict:
    require_pitch_access(user, pitch_id)
    require_bookable_date(payload.slot_date)
    if time.fromisoformat(payload.ends_at) <= time.fromisoformat(payload.starts_at):
        raise HTTPException(status_code=422, detail="Giờ kết thúc phải sau giờ bắt đầu.")
    client = get_supabase()
    pitch = client.table("pitches").select("id").eq("id", pitch_id).eq("is_active", True).maybe_single().execute().data
    if not pitch:
        raise HTTPException(status_code=404, detail="Không tìm thấy sân đang hoạt động.")
    try:
        return client.table("time_slots").insert({
            "id": f"{pitch_id}_{uuid4().hex}", "pitch_id": pitch_id, "slot_date": payload.slot_date.isoformat(),
            "starts_at": payload.starts_at, "ends_at": payload.ends_at, "price": payload.price, "status": "available",
        }).execute().data[0]
    except Exception as exc:
        if "23P01" in str(exc) or "23505" in str(exc):
            raise HTTPException(status_code=409, detail="Khung giờ trùng hoặc chồng lấn với lịch sân hiện có.") from exc
        raise mutation_error(exc) from exc


@router.get("/pitches/{pitch_id}/slots")
def list_pitch_slots(pitch_id: str, slot_date: date = Query(), user: CurrentUser = Depends(require_staff)) -> list[dict]:
    require_pitch_access(user, pitch_id)
    return (get_supabase().table("time_slots").select("*").eq("pitch_id", pitch_id)
            .eq("slot_date", slot_date.isoformat()).order("starts_at").execute().data)


@router.patch("/slots/{slot_id}")
def update_slot(slot_id: str, payload: SlotStatusUpdate, user: CurrentUser = Depends(require_staff)) -> dict:
    client = get_supabase()
    slot = client.table("time_slots").select("pitch_id").eq("id", slot_id).maybe_single().execute().data
    if not slot:
        raise HTTPException(status_code=404, detail="Không tìm thấy khung giờ.")
    require_pitch_access(user, slot["pitch_id"])
    result = client.table("time_slots").update({"status": payload.status}).eq("id", slot_id).neq("status", "booked").execute()
    if not result.data:
        raise HTTPException(status_code=409, detail="Khung giờ đang có lịch đặt.")
    return result.data[0]
