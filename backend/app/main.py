from fastapi import FastAPI
from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import account, admin, bookings, catalog
from app.supabase_client import close_supabase, get_supabase

settings = get_settings()
@asynccontextmanager
async def lifespan(app):
    if settings.supabase_url and settings.supabase_secret_key:
        get_supabase()
    try:
        yield
    finally:
        close_supabase()


app = FastAPI(title="SanBong API", version="1.0.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "apikey"],
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "sanbong-api"}


app.include_router(account.router, prefix="/v1")
app.include_router(bookings.router, prefix="/v1")
app.include_router(catalog.router, prefix="/v1")
app.include_router(admin.router, prefix="/v1")
