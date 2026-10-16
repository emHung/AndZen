from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

from fastapi import HTTPException

BOOKING_ADVANCE_DAYS = 7
BUSINESS_TIMEZONE = ZoneInfo("Asia/Ho_Chi_Minh")


def bookable_date_range() -> tuple[date, date]:
    today = datetime.now(BUSINESS_TIMEZONE).date()
    return today, today + timedelta(days=BOOKING_ADVANCE_DAYS)


def require_bookable_date(value: date) -> None:
    first_day, last_day = bookable_date_range()
    if value < first_day:
        raise HTTPException(status_code=422, detail="Không thể đặt sân cho ngày đã qua.")
    if value > last_day:
        raise HTTPException(status_code=422, detail="Chỉ có thể đặt sân trong 7 ngày tới.")
