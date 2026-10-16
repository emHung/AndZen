from dataclasses import dataclass
from functools import lru_cache
import logging

import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import get_settings
from app.supabase_client import get_supabase, read_query

bearer = HTTPBearer(auto_error=False)
logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def signing_keys() -> jwt.PyJWKClient:
    settings = get_settings()
    issuer = settings.supabase_issuer
    if issuer == "/auth/v1":
        raise HTTPException(status_code=503, detail="Chưa cấu hình URL Supabase.")
    return jwt.PyJWKClient(settings.resolved_jwks_url, cache_keys=True)


@dataclass(frozen=True)
class CurrentUser:
    id: str
    email: str | None
    phone: str | None
    role: str


def require_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
) -> CurrentUser:
    if credentials is None:
        raise HTTPException(status_code=401, detail="Vui lòng đăng nhập.")
    settings = get_settings()
    if not settings.supabase_url or not settings.supabase_secret_key:
        raise HTTPException(status_code=503, detail="Backend chưa được cấu hình Supabase.")
    try:
        signing_key = signing_keys().get_signing_key_from_jwt(credentials.credentials).key
        claims = jwt.decode(
            credentials.credentials,
            signing_key,
            algorithms=["ES256", "RS256"],
            audience="authenticated",
            issuer=settings.supabase_issuer,
            options={"require": ["exp", "sub", "iss", "aud"]},
        )
        user_id = claims["sub"]
        profile = read_query(
            get_supabase()
            .table("profiles")
            .select("role")
            .eq("id", user_id)
            .maybe_single()
        ).data
        if not profile:
            raise HTTPException(status_code=401, detail="Tài khoản chưa có hồ sơ hợp lệ.")
        return CurrentUser(
            id=user_id,
            email=claims.get("email"),
            phone=claims.get("phone"),
            role=profile.get("role", "customer"),
        )
    except HTTPException:
        raise
    except jwt.exceptions.PyJWKClientConnectionError as exc:
        raise HTTPException(status_code=503, detail="Không kết nối được dịch vụ xác thực. Vui lòng thử lại.") from exc
    except (jwt.InvalidTokenError, jwt.exceptions.PyJWKClientError) as exc:
        raise HTTPException(status_code=401, detail="Phiên đăng nhập không hợp lệ hoặc đã hết hạn.") from exc
    except Exception as exc:
        logger.exception("Authentication dependency unavailable")
        raise HTTPException(status_code=503, detail="Không tải được thông tin tài khoản. Vui lòng thử lại.") from exc


def require_staff(user: CurrentUser = Depends(require_user)) -> CurrentUser:
    if user.role not in {"staff", "admin"}:
        raise HTTPException(status_code=403, detail="Chỉ nhân viên sân mới được thực hiện thao tác này.")
    return user


def require_admin(user: CurrentUser = Depends(require_user)) -> CurrentUser:
    if user.role != "admin":
        raise HTTPException(status_code=403, detail="Chỉ quản trị viên mới được thực hiện thao tác này.")
    return user

