from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class ProfileUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    full_name: str = Field(min_length=2, max_length=100)
    email: str | None = Field(default=None, max_length=254)
    phone: str | None = Field(default=None, pattern=r"^0[0-9]{9}$")
    district: str | None = Field(default=None, max_length=120)
    gender: Literal["Nam", "Nữ", "Khác"] | None = None
    birthdate: date | None = None


class EmailChange(BaseModel):
    model_config = ConfigDict(extra="forbid")
    email: str = Field(max_length=254, pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


class StaffAssignments(BaseModel):
    model_config = ConfigDict(extra="forbid")
    pitch_ids: list[str] = Field(max_length=100)


class SettingsUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    notifications_enabled: bool
    profile_public: bool


class BookingCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    pitch_id: str = Field(min_length=1, max_length=40)
    slot_id: str = Field(min_length=1, max_length=160)
    booking_date: date
    customer_name: str = Field(min_length=2, max_length=100)
    customer_phone: str = Field(pattern=r"^0[0-9]{9}$")
    payment_method: Literal["cash", "qr"]


class PitchCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: str = Field(min_length=2, max_length=40, pattern=r"^[a-zA-Z0-9_-]+$")
    name: str = Field(min_length=2, max_length=120)
    district: str = Field(min_length=2, max_length=120)
    address: str = Field(min_length=5, max_length=250)
    pitch_type: Literal["FIVE", "SEVEN", "ELEVEN"]
    base_price: int = Field(gt=0, le=100_000_000)
    description: str = Field(default="", max_length=2000)
    rating: float = Field(default=0, ge=0, le=5)
    amenities: list[str] = Field(default_factory=list, max_length=30)
    image_key: str | None = Field(default=None, max_length=120)
    is_active: bool = True


class PitchUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    name: str = Field(min_length=2, max_length=120)
    district: str = Field(min_length=2, max_length=120)
    address: str = Field(min_length=5, max_length=250)
    pitch_type: Literal["FIVE", "SEVEN", "ELEVEN"]
    base_price: int = Field(gt=0, le=100_000_000)
    description: str = Field(default="", max_length=2000)
    rating: float = Field(ge=0, le=5)
    amenities: list[str] = Field(max_length=30)
    image_key: str | None = Field(default=None, max_length=120)
    is_active: bool = True


class SlotStatusUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    status: Literal["available", "unavailable"]


class SlotCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    slot_date: date
    starts_at: str = Field(pattern=r"^([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$")
    ends_at: str = Field(pattern=r"^([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$")
    price: int = Field(gt=0, le=100_000_000)


class BookingStatusUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    status: Literal["confirmed", "cancelled"]


class PaymentStatusUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    payment_status: Literal["paid", "cash_due", "qr_due"]

