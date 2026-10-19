from datetime import date, datetime, time

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials

from app.booking_window import BUSINESS_TIMEZONE, bookable_date_range, require_bookable_date
from app.security import CurrentUser, authenticate_token, bearer, require_user
from app.supabase_client import get_public_supabase, get_user_supabase, read_query

router = APIRouter(tags=["catalog"])


def optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
) -> CurrentUser | None:
    if credentials is None:
        return None
    return authenticate_token(credentials.credentials)


def window_slots(slot_date: date | None = None, client=None) -> list[dict]:
    if slot_date is not None:
        require_bookable_date(slot_date)
    first, last = bookable_date_range()
    query = (client or get_public_supabase()).table("time_slots").select("pitch_id,slot_date,starts_at,ends_at,status,pitches!inner(is_active)")
    query = query.eq("pitches.is_active", True)
    if slot_date:
        query = query.eq("slot_date", slot_date.isoformat())
    else:
        query = query.gte("slot_date", first.isoformat()).lte("slot_date", last.isoformat())
    return read_query(query).data


@router.get("/time-ranges")
def list_time_ranges(slot_date: date | None = Query(None, alias="date")) -> list[str]:
    return sorted({f"{r['starts_at'][:5]} - {r['ends_at'][:5]}" for r in window_slots(slot_date)})


@router.get("/availability")
def available_pitches(slot_date: date | None = Query(None, alias="date"),
                      time_range: str | None = Query(None, max_length=30),
                      user: CurrentUser = Depends(require_user)) -> list[str]:
    local_now = datetime.now(BUSINESS_TIMEZONE)
    return sorted({r["pitch_id"] for r in window_slots(slot_date, get_user_supabase(user.access_token))
                   if r["status"] == "available"
                   and datetime.combine(date.fromisoformat(r["slot_date"]), time.fromisoformat(r["starts_at"]), BUSINESS_TIMEZONE) > local_now
                   and (time_range is None or f"{r['starts_at'][:5]} - {r['ends_at'][:5]}" == time_range)})


@router.get("/pitches")
def list_pitches(
    pitch_type: str | None = Query(default=None, alias="type", pattern="^(FIVE|SEVEN|ELEVEN)$"),
    district: str | None = Query(default=None, max_length=120),
    max_price: int | None = Query(default=None, gt=0, le=100_000_000),
    user: CurrentUser | None = Depends(optional_user),
) -> list[dict]:
    client = get_user_supabase(user.access_token) if user else get_public_supabase()
    query = client.table("pitches").select("*").eq("is_active", True)
    if pitch_type:
        query = query.eq("pitch_type", pitch_type)
    if district:
        query = query.ilike("district", district.strip())
    if max_price is not None:
        query = query.lte("base_price", max_price)
    return read_query(query.order("id")).data


@router.get("/pitches/{pitch_id}/slots")
def list_slots(
    pitch_id: str,
    slot_date: date = Query(alias="date"),
    user: CurrentUser = Depends(require_user),
) -> list[dict]:
    require_bookable_date(slot_date)
    client = get_user_supabase(user.access_token)
    pitch = read_query(client.table("pitches").select("id").eq("id", pitch_id).eq("is_active", True).maybe_single()).data
    if not pitch:
        raise HTTPException(status_code=404, detail="Không tìm thấy sân.")
    rows = read_query(
        client.table("time_slots").select("*").eq("pitch_id", pitch_id).eq("slot_date", slot_date.isoformat())
        .order("starts_at")
    ).data
    local_now = datetime.now(BUSINESS_TIMEZONE)
    for row in rows:
        starts = datetime.combine(slot_date, time.fromisoformat(row["starts_at"]), BUSINESS_TIMEZONE)
        if row["status"] == "available" and starts <= local_now:
            row["status"] = "unavailable"
    return rows


@router.get("/favorites")
def list_favorites(user: CurrentUser = Depends(require_user)) -> list[str]:
    rows = get_user_supabase(user.access_token).table("favorites").select("pitch_id").eq("user_id", user.id).execute().data
    return [row["pitch_id"] for row in rows]


@router.put("/favorites/{pitch_id}")
def set_favorite(pitch_id: str, enabled: bool, user: CurrentUser = Depends(require_user)) -> dict[str, bool]:
    client = get_user_supabase(user.access_token)
    if enabled:
        client.table("favorites").upsert({"user_id": user.id, "pitch_id": pitch_id}, on_conflict="user_id,pitch_id", ignore_duplicates=True).execute()
    else:
        client.table("favorites").delete().eq("user_id", user.id).eq("pitch_id", pitch_id).execute()
    return {"is_favorite": enabled}

