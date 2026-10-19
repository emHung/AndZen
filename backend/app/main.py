import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from starlette.responses import JSONResponse

from app.config import get_settings
from app.routers import account, admin, bookings, catalog
from app.supabase_client import close_supabase, get_supabase

settings = get_settings()
logger = logging.getLogger(__name__)
@asynccontextmanager
async def lifespan(app):
    if settings.supabase_url and settings.supabase_secret_key:
        get_supabase()
    try:
        yield
    finally:
        close_supabase()


app = FastAPI(title="SanBong API", version="1.0.0", lifespan=lifespan)


@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    errors = [{"loc": error.get("loc", []), "msg": error.get("msg", "Giá trị không hợp lệ."),
               "type": error.get("type", "value_error")} for error in exc.errors()]
    return JSONResponse(status_code=422, content={"detail": errors})


@app.exception_handler(Exception)
async def unexpected_error_handler(request: Request, exc: Exception):
    logger.error("Unhandled API exception (%s)", type(exc).__name__)
    return JSONResponse(status_code=500, content={"detail": "Đã xảy ra lỗi hệ thống. Vui lòng thử lại."})


app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "apikey"],
)


class RequestBodyTooLarge(Exception):
    pass


class RequestSecurityMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        content_length = headers.get(b"content-length")
        if content_length:
            try:
                if int(content_length) > settings.max_request_body_bytes:
                    await self._reject(send, 413, "Yêu cầu vượt quá dung lượng cho phép.")
                    return
            except ValueError:
                await self._reject(send, 400, "Yêu cầu không hợp lệ.")
                return

        received = 0

        async def limited_receive():
            nonlocal received
            message = await receive()
            if message["type"] == "http.request":
                received += len(message.get("body", b""))
                if received > settings.max_request_body_bytes:
                    raise RequestBodyTooLarge
            return message

        async def secured_send(message):
            if message["type"] == "http.response.start":
                response_headers = list(message.get("headers", []))
                response_headers.extend([
                    (b"x-content-type-options", b"nosniff"),
                    (b"x-frame-options", b"DENY"),
                    (b"referrer-policy", b"no-referrer"),
                    (b"permissions-policy", b"camera=(), microphone=(), geolocation=()"),
                    (b"content-security-policy", b"default-src 'none'; frame-ancestors 'none'"),
                ])
                response_headers.append((b"strict-transport-security", b"max-age=31536000; includeSubDomains"))
                message["headers"] = response_headers
            await send(message)

        try:
            await self.app(scope, limited_receive, secured_send)
        except RequestBodyTooLarge:
            await self._reject(send, 413, "Yêu cầu vượt quá dung lượng cho phép.")

    @staticmethod
    async def _reject(send, status, detail):
        body = (f'{{"detail":"{detail}"}}').encode("utf-8")
        await send({"type": "http.response.start", "status": status, "headers": [
            (b"content-type", b"application/json; charset=utf-8"),
            (b"content-length", str(len(body)).encode("ascii")),
            (b"x-content-type-options", b"nosniff"),
            (b"x-frame-options", b"DENY"),
            (b"referrer-policy", b"no-referrer"),
            (b"permissions-policy", b"camera=(), microphone=(), geolocation=()"),
            (b"content-security-policy", b"default-src 'none'; frame-ancestors 'none'"),
            (b"strict-transport-security", b"max-age=31536000; includeSubDomains"),
        ]})
        await send({"type": "http.response.body", "body": body})


app.add_middleware(RequestSecurityMiddleware)


@app.get("/health")
def health() -> dict[str, str]:
    try:
        get_supabase().table("pitches").select("id").limit(1).execute()
    except Exception:
        return JSONResponse(status_code=503, content={"status": "unavailable", "service": "sanbong-api"})
    return {"status": "ok", "service": "sanbong-api", "database": "ok"}


app.include_router(account.router, prefix="/v1")
app.include_router(bookings.router, prefix="/v1")
app.include_router(catalog.router, prefix="/v1")
app.include_router(admin.router, prefix="/v1")
