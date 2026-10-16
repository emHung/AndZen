from fastapi import HTTPException

from app.security import CurrentUser
from app.supabase_client import get_supabase


def managed_pitch_ids(user: CurrentUser) -> list[str] | None:
    if user.role == "admin":
        return None
    rows = get_supabase().table("staff_pitch_assignments").select("pitch_id").eq("user_id", user.id).execute().data
    return [row["pitch_id"] for row in rows]


def require_pitch_access(user: CurrentUser, pitch_id: str) -> None:
    allowed = managed_pitch_ids(user)
    if allowed is not None and pitch_id not in allowed:
        raise HTTPException(status_code=403, detail="Bạn chưa được phân công quản lý sân này.")


def mutation_error(exc: Exception) -> HTTPException:
    message = str(exc)
    mappings = {
        "permission_denied": (403, "Bạn chưa được phân công quản lý sân này."),
        "booking_not_found": (404, "Không tìm thấy lịch đặt."),
        "staff_not_found": (404, "Không tìm thấy tài khoản nhân viên."),
        "pitch_not_found": (404, "Không tìm thấy sân."),
        "slot_unavailable": (409, "Khung giờ không còn trống."),
        "booking_not_cancellable": (409, "Lịch đã hủy hoặc đã bắt đầu; không thể thực hiện thao tác."),
        "booking_date_too_far": (422, "Chỉ có thể đặt sân trong 7 ngày tới."),
        "invalid_status": (422, "Trạng thái không phù hợp với lịch đặt."),
    }
    for code, (status, detail) in mappings.items():
        if code in message:
            return HTTPException(status_code=status, detail=detail)
    return HTTPException(status_code=502, detail="Không cập nhật được dữ liệu. Vui lòng thử lại.")
