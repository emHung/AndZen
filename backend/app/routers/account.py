from uuid import uuid4
from datetime import datetime, timezone
from io import BytesIO

import httpx
from PIL import Image, UnidentifiedImageError
from fastapi import APIRouter, Depends, File, HTTPException, Response, UploadFile
from fastapi.security import HTTPAuthorizationCredentials

from app.config import get_settings as get_app_settings
from app.schemas import EmailChange, ProfileUpdate, SettingsUpdate
from app.security import CurrentUser, bearer, require_user
from app.supabase_client import get_supabase, get_user_supabase

router = APIRouter(tags=["account"])
AVATAR_MAX_BYTES = 5 * 1024 * 1024
AVATAR_TYPES = {"image/jpeg", "image/png", "image/webp"}
AVATAR_URL_TTL_SECONDS = 3600


def with_avatar_url(profile: dict | None, client) -> dict:
    if not profile:
        return profile
    result = dict(profile)
    avatar_path = result.get("avatar_path")
    if avatar_path:
        signed = client.storage.from_(get_app_settings().avatar_bucket).create_signed_url(
            avatar_path, AVATAR_URL_TTL_SECONDS
        )
        result["avatar_url"] = signed.get("signedURL") or signed.get("signedUrl") or signed.get("signed_url")
    return result


@router.get("/me")
def get_profile(user: CurrentUser = Depends(require_user)) -> dict:
    client = get_user_supabase(user.access_token)
    result = client.table("profiles").select("*").eq("id", user.id).single().execute()
    return with_avatar_url(result.data, client)


@router.patch("/me")
def update_profile(payload: ProfileUpdate, user: CurrentUser = Depends(require_user)) -> dict:
    values = payload.model_dump(mode="json", exclude={"email"})
    values["updated_at"] = datetime.now(timezone.utc).isoformat()
    client = get_user_supabase(user.access_token)
    if payload.email is not None:
        if payload.email.strip().lower() != (user.email or "").lower():
            raise HTTPException(status_code=422, detail="Dùng chức năng đổi email và xác nhận email trước khi cập nhật hồ sơ.")
    result = client.table("profiles").update(values).eq("id", user.id).execute()
    if not result.data:
        raise HTTPException(status_code=404, detail="Không tìm thấy hồ sơ.")
    return result.data[0]


@router.post("/me/email")
def change_email(payload: EmailChange, user: CurrentUser = Depends(require_user),
                 credentials: HTTPAuthorizationCredentials = Depends(bearer)) -> dict:
    settings = get_app_settings()
    current = get_supabase().auth.admin.get_user_by_id(user.id).user.email
    if payload.email.lower() == (current or "").lower():
        return {"email": current, "email_change_pending": False}
    # User bearer preserves Supabase's email confirmation flow.
    try:
        response = httpx.put(f"{settings.supabase_issuer}/user", headers={
            "apikey": settings.supabase_publishable_key, "Authorization": f"Bearer {credentials.credentials}"
        }, json={"email": payload.email}, timeout=15)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Không gửi được yêu cầu đổi email. Hồ sơ vẫn dùng email hiện tại.") from exc
    if response.status_code >= 400:
        raise HTTPException(status_code=422, detail="Không đổi được email. Kiểm tra địa chỉ và cấu hình gửi email.")
    actual = response.json().get("email", current)
    return {"email": actual, "email_change_pending": actual != payload.email}


@router.get("/me/settings")
def get_settings(user: CurrentUser = Depends(require_user)) -> dict:
    result = get_user_supabase(user.access_token).table("user_settings").select("*").eq("user_id", user.id).maybe_single().execute()
    return result.data or {"notifications_enabled": True, "profile_public": False}


@router.put("/me/settings")
def update_settings(payload: SettingsUpdate, user: CurrentUser = Depends(require_user)) -> dict:
    values = {"user_id": user.id, **payload.model_dump()}
    result = get_user_supabase(user.access_token).table("user_settings").upsert(values, on_conflict="user_id").execute()
    return result.data[0]


@router.post("/me/avatar")
def upload_avatar(file: UploadFile = File(...), user: CurrentUser = Depends(require_user)) -> dict[str, str]:
    if file.content_type not in AVATAR_TYPES:
        raise HTTPException(status_code=415, detail="Ảnh đại diện chỉ nhận JPEG, PNG hoặc WebP.")
    content = file.file.read(AVATAR_MAX_BYTES + 1)
    if len(content) > AVATAR_MAX_BYTES:
        raise HTTPException(status_code=413, detail="Ảnh đại diện tối đa 5 MB.")
    try:
        with Image.open(BytesIO(content)) as image:
            detected_format = image.format
            if image.width * image.height > 20_000_000:
                raise HTTPException(status_code=413, detail="Kích thước ảnh quá lớn.")
            image.verify()
    except HTTPException:
        raise
    except (UnidentifiedImageError, Image.DecompressionBombError, OSError, ValueError):
        raise HTTPException(status_code=415, detail="Tệp tải lên không phải ảnh hợp lệ.")
    format_types = {"JPEG": "image/jpeg", "PNG": "image/png", "WEBP": "image/webp"}
    if format_types.get(detected_format) != file.content_type:
        raise HTTPException(status_code=415, detail="Định dạng ảnh không khớp nội dung tệp.")
    suffix = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}[file.content_type]
    path = f"{user.id}/{uuid4().hex}.{suffix}"
    client = get_user_supabase(user.access_token)
    try:
        old_profile = client.table("profiles").select("avatar_path").eq("id", user.id).maybe_single().execute().data
        client.storage.from_(get_app_settings().avatar_bucket).upload(
            path,
            content,
            file_options={"content-type": file.content_type, "upsert": "false"},
        )
        signed_url = client.storage.from_(get_app_settings().avatar_bucket).create_signed_url(path, AVATAR_URL_TTL_SECONDS)
        avatar_url = signed_url.get("signedURL") or signed_url.get("signedUrl") or signed_url.get("signed_url")
        updated = client.table("profiles").update({"avatar_path": path, "avatar_url": None}).eq("id", user.id).execute()
        if not updated.data:
            client.storage.from_(get_app_settings().avatar_bucket).remove([path])
            raise RuntimeError("profile_missing")
        if old_profile and old_profile.get("avatar_path"):
            client.storage.from_(get_app_settings().avatar_bucket).remove([old_profile["avatar_path"]])
        return {"avatar_path": path, "avatar_url": avatar_url}
    except Exception:
        raise HTTPException(status_code=502, detail="Không tải được ảnh lên kho lưu trữ.") from None


@router.delete("/me", status_code=204)
def delete_account(user: CurrentUser = Depends(require_user)) -> Response:
    client = get_supabase()
    profile = client.table("profiles").select("avatar_path").eq("id", user.id).maybe_single().execute().data
    try:
        if profile and profile.get("avatar_path"):
            client.storage.from_(get_app_settings().avatar_bucket).remove([profile["avatar_path"]])
        client.auth.admin.delete_user(user.id)
    except Exception as exc:
        raise HTTPException(status_code=502, detail="Không xóa được tài khoản. Vui lòng thử lại.") from exc
    return Response(status_code=204)

