from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_env: str = "development"
    api_host: str = "0.0.0.0"
    api_port: int = 8000
    supabase_url: str = ""
    supabase_publishable_key: str = ""
    supabase_secret_key: str = ""
    supabase_jwks_url: str = ""
    cors_origins: list[str] = Field(default_factory=list)
    avatar_bucket: str = "avatars"
    supabase_timeout_seconds: float = 30
    supabase_read_retries: int = Field(default=2, ge=0, le=3)
    supabase_retry_delay_seconds: float = 0.2
    supabase_connect_timeout_seconds: float = 10
    supabase_pool_connections: int = 20
    supabase_pool_keepalive: int = 5
    supabase_keepalive_expiry_seconds: float = Field(default=30, gt=0)
    max_request_body_bytes: int = Field(default=6 * 1024 * 1024, gt=0)

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @property
    def supabase_issuer(self) -> str:
        return f"{self.supabase_url.rstrip('/')}/auth/v1"

    @property
    def resolved_jwks_url(self) -> str:
        return self.supabase_jwks_url or f"{self.supabase_issuer}/.well-known/jwks.json"


@lru_cache
def get_settings() -> Settings:
    return Settings()

