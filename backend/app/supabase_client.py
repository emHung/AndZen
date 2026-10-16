from functools import lru_cache

from fastapi import HTTPException
import time
import httpx
from supabase.lib.client_options import SyncClientOptions
from supabase import Client, create_client

from app.config import get_settings


@lru_cache(maxsize=1)
def transport() -> httpx.Client:
    settings = get_settings()
    # SDK defaults to HTTP/2. HTTP/1.1 avoids the Windows SSL EWOULDBLOCK
    # observed during parallel date queries; reuse a bounded connection pool.
    return httpx.Client(http2=False, timeout=httpx.Timeout(settings.supabase_timeout_seconds, connect=settings.supabase_connect_timeout_seconds),
                        limits=httpx.Limits(max_connections=settings.supabase_pool_connections,
                                            max_keepalive_connections=settings.supabase_pool_keepalive,
                                            keepalive_expiry=settings.supabase_keepalive_expiry_seconds))


@lru_cache(maxsize=1)
def get_supabase() -> Client:
    settings = get_settings()
    if not settings.supabase_url or not settings.supabase_secret_key:
        raise HTTPException(
            status_code=503,
            detail="Backend chưa được cấu hình Supabase. Hãy điền SUPABASE_URL và SUPABASE_SECRET_KEY.",
        )
    return create_client(settings.supabase_url, settings.supabase_secret_key,
                         options=SyncClientOptions(httpx_client=transport(), auto_refresh_token=False, persist_session=False))


def read_query(query):
    settings = get_settings()
    for attempt in range(settings.supabase_read_retries + 1):
        try:
            return query.execute()
        except httpx.TransportError as exc:
            if attempt == settings.supabase_read_retries:
                raise HTTPException(status_code=503, detail="Không kết nối được dữ liệu sân. Vui lòng thử lại.") from exc
            time.sleep(settings.supabase_retry_delay_seconds * 2 ** attempt)


def close_supabase() -> None:
    if transport.cache_info().currsize:
        transport().close()
    get_supabase.cache_clear()
    transport.cache_clear()

