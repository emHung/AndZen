"""Idempotent fixture setup and integration QA. Default is a read-only preview.

Run from backend: .venv/Scripts/python.exe qa_booking_flow.py --apply
Secrets are read from .env and never written to reports. Auth links are generated
locally through the admin API; no invitation emails or password resets are sent.
"""
import argparse
import base64
import json
import secrets
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
from pathlib import Path
from uuid import NAMESPACE_DNS, uuid5
import sys

import httpx
from dotenv import dotenv_values
from supabase import create_client
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parent
sys.stdout.reconfigure(encoding="utf-8")
TODAY = datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")).date()
RANGES = [("06:00", "07:30"), ("07:30", "09:00"), ("09:00", "10:30"),
          ("10:30", "12:00"), ("12:00", "13:30"), ("13:30", "15:00"),
          ("15:00", "16:30"), ("16:30", "18:00"), ("18:00", "19:30"),
          ("19:30", "21:00"), ("21:00", "22:30")]
TEST_PITCHES = ["test_a", "test_b"]
checks = []
temporary_bookings = []


def check(label, condition):
    if not condition:
        raise AssertionError(label)
    checks.append(label)
    print(json.dumps({"check": label, "result": "passed"}, ensure_ascii=False), flush=True)


def slot_id(pitch, day, index):
    return f"{pitch}_{day.isoformat()}_{index:02d}"


def user_token(admin, publishable, email):
    link = admin.auth.admin.generate_link({"type": "magiclink", "email": email})
    reply = publishable.auth.verify_otp({"type": "magiclink", "token_hash": link.properties.hashed_token})
    if not reply.session:
        raise RuntimeError("Could not establish QA session")
    return reply.session.access_token


def request(base, token, method, path, body=None, expected=200):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    with httpx.Client(timeout=45) as client:
        reply = client.request(method, base + path, json=body, headers=headers)
    if reply.status_code != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {reply.status_code}: {reply.text[:250]}")
    return reply.json() if reply.content else None


def booking_body(pitch, day, index, name="[QA FLOW]", method="cash"):
    return {"pitch_id": pitch, "slot_id": slot_id(pitch, day, index), "booking_date": day.isoformat(),
            "customer_name": name, "customer_phone": "0900000001", "payment_method": method}


def track(row):
    temporary_bookings.append(row["id"])
    return row


def seed(admin, base, admin_token, people, tokens):
    existing = {p["id"]: p for p in admin.table("pitches").select("id,name").execute().data}
    for index, pitch in enumerate(TEST_PITCHES):
        if pitch in existing:
            if not existing[pitch]["name"].startswith("[TEST]"):
                raise RuntimeError("A test pitch identifier belongs to non-test data")
            continue
        request(base, admin_token, "POST", "/v1/admin/pitches", {
            "id": pitch, "name": f"[TEST] Sân {'Thanh Xuân' if index == 0 else 'Cầu Giấy'}",
            "district": "Thanh Xuân" if index == 0 else "Cầu Giấy",
            "address": "[TEST] Địa chỉ thử nghiệm, Hà Nội", "pitch_type": "SEVEN" if index == 0 else "FIVE",
            "base_price": 300000 + index * 50000, "rating": 0, "amenities": ["Đèn LED", "Bãi xe"],
            "description": "Dữ liệu thử nghiệm để kiểm tra đặt, đổi và hủy lịch. Không phải sân thương mại.",
            "image_key": "img_pitch_1" if index == 0 else "img_pitch_2", "is_active": True,
        }, expected=201)
    known = {s["id"] for s in admin.table("time_slots").select("id").in_("pitch_id", TEST_PITCHES).execute().data}
    rows = []
    for pitch in TEST_PITCHES:
        for day_offset in range(-1, 8):
            day = TODAY + timedelta(days=day_offset)
            for i, (start, end) in enumerate(RANGES):
                identifier = slot_id(pitch, day, i)
                if identifier not in known:
                    rows.append({"id": identifier, "pitch_id": pitch, "slot_date": day.isoformat(),
                                 "starts_at": start, "ends_at": end, "price": 300000 if i < 8 else 350000,
                                 "status": "available"})
    if rows:
        admin.table("time_slots").insert(rows).execute()
    past_id = f"test_a_{TODAY}_past"
    if not admin.table("time_slots").select("id").eq("id", past_id).execute().data:
        admin.table("time_slots").insert({"id": past_id, "pitch_id": "test_a", "slot_date": str(TODAY),
            "starts_at": "00:00", "ends_at": "01:30", "price": 300000, "status": "available"}).execute()
    # One fully blocked day makes the exhausted-day state visible in the app.
    admin.table("time_slots").update({"status": "unavailable"}).eq("pitch_id", "test_b").eq(
        "slot_date", str(TODAY+timedelta(days=7))).eq("status", "available").execute()
    staff = next(p for p in people if p["role"] == "staff")
    request(base, admin_token, "PUT", f"/v1/admin/staff/{staff['id']}/pitches", {"pitch_ids": ["test_a"]})
    for i, person in enumerate(people):
        token = tokens[person["id"]]
        label = f"[TEST] {person['full_name'] or 'Tài khoản'}"
        for pitch, offset, cancel in [("test_a", 1, False), ("test_b", 2, True)]:
            day = TODAY + timedelta(days=offset)
            sid = slot_id(pitch, day, i)
            existing_booking = admin.table("bookings").select("id").eq("user_id", person["id"]).eq("slot_id", sid).eq("customer_name", label).execute().data
            if not existing_booking:
                row = request(base, token, "POST", "/v1/bookings", booking_body(pitch, day, i, label, "cash" if i % 2 == 0 else "qr"), 201)
                if cancel:
                    request(base, token, "POST", f"/v1/bookings/{row['id']}/cancel")
        yesterday = TODAY - timedelta(days=1)
        history_id = str(uuid5(NAMESPACE_DNS, f"andzen.test.history.{person['id']}.{yesterday}"))
        if not admin.table("bookings").select("id").eq("id", history_id).execute().data:
            start, end = RANGES[i]
            admin.table("bookings").insert({"id": history_id, "booking_code": "QA-" + history_id[:8].upper(),
                "user_id": person["id"], "pitch_id": "test_a", "slot_id": slot_id("test_a", yesterday, i),
                "booking_date": yesterday.isoformat(), "pitch_name": "[TEST] Sân Thanh Xuân",
                "pitch_address": "[TEST] Địa chỉ thử nghiệm, Hà Nội", "time_range": f"{start} - {end}",
                "total_price": 300000, "customer_name": label, "customer_phone": "0900000001",
                "payment_method": "cash", "payment_status": "paid", "status": "confirmed"}).execute()
            admin.table("time_slots").update({"status": "booked"}).eq("id", slot_id("test_a", yesterday, i)).execute()
        request(base, token, "PUT", "/v1/favorites/test_a?enabled=true", {})
    check("fixtures_for_every_account", True)


def integration(admin, base, admin_token, people, tokens):
    customers = [p for p in people if p["role"] == "customer"]
    person = next((p for p in customers if p["email"].startswith("andzen.qa.")), customers[0])
    customer = tokens[person["id"]]
    other = tokens[next(p for p in customers if p["id"] != person["id"])["id"]]
    staff_person = next(p for p in people if p["role"] == "staff")
    staff = tokens[staff_person["id"]]
    tomorrow, next_day = TODAY + timedelta(days=1), TODAY + timedelta(days=2)
    row = track(request(base, customer, "POST", "/v1/bookings", booking_body("test_a", tomorrow, 4), 201))
    check("cash_confirmed_and_slot_booked", row["status"] == "confirmed" and row["payment_status"] == "cash_due")
    listed = request(base, customer, "GET", "/v1/bookings")
    check("booking_metadata_comes_from_database", next(b for b in listed if b["id"] == row["id"])["pitches"]["pitch_type"] == "SEVEN")
    def load_day(offset):
        return request(base, customer, "GET", f"/v1/pitches/test_a/slots?date={TODAY+timedelta(days=offset)}")
    with ThreadPoolExecutor(max_workers=8) as pool:
        days = list(pool.map(load_day, range(8)))
    check("eight_parallel_calendar_requests_complete", len(days) == 8 and all(isinstance(day, list) for day in days))
    ranges = request(base, None, "GET", f"/v1/time-ranges?date={tomorrow}")
    check("filters_include_non_default_database_hours", "12:00 - 13:30" in ranges and "09:00 - 10:30" in ranges)
    matching = request(base, customer, "GET", f"/v1/availability?date={tomorrow}&time_range=12:00%20-%2013:30")
    check("time_range_filter_excludes_booked_pitch", "test_a" not in matching and "test_b" in matching)
    check("unknown_time_range_has_no_results", request(base, customer, "GET", "/v1/availability?time_range=03:00%20-%2004:00") == [])
    request(base, other, "POST", "/v1/bookings", booking_body("test_a", tomorrow, 4), expected=409)
    check("duplicate_slot_rejected", True)
    request(base, other, "POST", f"/v1/bookings/{row['id']}/cancel", expected=404)
    check("customer_cannot_cancel_other_account", True)
    same = request(base, customer, "POST", f"/v1/bookings/{row['id']}/reschedule?slot_id={row['slot_id']}&booking_date={tomorrow}")
    check("same_slot_reschedule_is_idempotent", same["slot_id"] == row["slot_id"])
    moved = request(base, customer, "POST", f"/v1/bookings/{row['id']}/reschedule?slot_id={slot_id('test_a', next_day, 5)}&booking_date={next_day}")
    old_status = admin.table("time_slots").select("status").eq("id", row["slot_id"]).single().execute().data["status"]
    new_status = admin.table("time_slots").select("status").eq("id", moved["slot_id"]).single().execute().data["status"]
    check("reschedule_changes_date_and_releases_old_slot", moved["booking_date"] == str(next_day) and old_status == "available" and new_status == "booked")
    request(base, staff, "PATCH", f"/v1/admin/bookings/{row['id']}/payment", {"payment_status": "paid"})
    check("assigned_staff_can_confirm_cash_receipt", True)
    request(base, staff, "PATCH", f"/v1/admin/bookings/{row['id']}", {"status": "cancelled"})
    freed = admin.table("time_slots").select("status").eq("id", moved["slot_id"]).single().execute().data["status"]
    check("staff_cancellation_releases_slot_atomically", freed == "available")
    request(base, staff, "PATCH", f"/v1/admin/bookings/{row['id']}/payment", {"payment_status": "paid"}, expected=409)
    request(base, admin_token, "PATCH", f"/v1/admin/bookings/{row['id']}", {"status": "confirmed"})
    status = admin.table("time_slots").select("status").eq("id", moved["slot_id"]).single().execute().data["status"]
    check("admin_reconfirmation_reserves_slot", status == "booked")
    request(base, customer, "POST", f"/v1/bookings/{row['id']}/cancel")
    request(base, customer, "POST", f"/v1/bookings/{row['id']}/cancel")
    check("customer_cancellation_is_idempotent", True)

    qr = track(request(base, customer, "POST", "/v1/bookings", booking_body("test_b", tomorrow, 4, method="qr"), 201))
    check("qr_option_is_confirmed_but_unpaid", qr["payment_status"] == "qr_due" and qr["status"] == "confirmed")
    request(base, staff, "PATCH", f"/v1/admin/bookings/{qr['id']}", {"status": "cancelled"}, expected=403)
    request(base, staff, "PATCH", f"/v1/admin/bookings/{qr['id']}/payment", {"payment_status": "paid"}, expected=403)
    request(base, staff, "GET", f"/v1/admin/pitches/test_b/slots?slot_date={tomorrow}", expected=403)
    staff_pitches = request(base, staff, "GET", "/v1/admin/pitches")
    staff_bookings = request(base, staff, "GET", "/v1/admin/bookings")
    check("staff_scope_limits_reads_and_writes", all(p["id"] == "test_a" for p in staff_pitches) and all(b["pitch_id"] == "test_a" for b in staff_bookings))
    request(base, customer, "GET", "/v1/admin/pitches", expected=403)
    request(base, customer, "PATCH", "/v1/me", {"full_name": "Invalid escalation", "role": "admin"}, expected=422)
    check("customer_cannot_manage_or_escalate_role", True)
    request(base, customer, "POST", f"/v1/bookings/{qr['id']}/cancel")

    request(base, admin_token, "POST", "/v1/admin/pitches/test_a/slots", {"slot_date": str(tomorrow),
        "starts_at": "06:45", "ends_at": "08:00", "price": 300000}, expected=409)
    check("overlapping_slots_rejected_by_database", True)
    request(base, customer, "POST", "/v1/bookings", booking_body("test_a", TODAY + timedelta(days=8), 6), expected=422)
    request(base, customer, "GET", f"/v1/pitches/test_a/slots?date={TODAY + timedelta(days=8)}", expected=422)
    boundary = track(request(base, customer, "POST", "/v1/bookings", booking_body("test_a", TODAY + timedelta(days=7), 6), 201))
    request(base, customer, "POST", f"/v1/bookings/{boundary['id']}/cancel")
    check("seven_day_boundary_enforced", True)
    past_slots = request(base, customer, "GET", f"/v1/pitches/test_a/slots?date={TODAY}")
    check("elapsed_slots_are_not_reported_available", next(r for r in past_slots if r["id"] == f"test_a_{TODAY}_past")["status"] == "unavailable")
    past_body = booking_body("test_a", TODAY, 0)
    past_body["slot_id"] = f"test_a_{TODAY}_past"
    request(base, customer, "POST", "/v1/bookings", past_body, expected=422)
    check("booking_elapsed_slot_is_rejected", True)
    exhausted = request(base, customer, "GET", f"/v1/pitches/test_b/slots?date={TODAY+timedelta(days=7)}")
    check("exhausted_day_has_zero_available_slots", exhausted and not any(s["status"] == "available" for s in exhausted))
    history = admin.table("bookings").select("id").eq("user_id", person["id"]).eq("booking_date", str(TODAY-timedelta(days=1))).execute().data
    request(base, customer, "POST", f"/v1/bookings/{history[0]['id']}/cancel", expected=409)
    check("completed_booking_cannot_be_cancelled", True)
    try:
        admin.rpc("create_booking", {"p_user_id": person["id"], "p_pitch_id": "test_a",
            "p_slot_id": slot_id("test_a", TODAY+timedelta(days=7), 6), "p_booking_date": str(TODAY+timedelta(days=8)),
            "p_customer_name": "[QA FLOW]", "p_customer_phone": "0900000001", "p_payment_method": "cash"}).execute()
    except Exception as exc:
        check("database_also_rejects_dates_beyond_window", "booking_date_too_far" in str(exc))
    else:
        raise AssertionError("Database booking window not enforced")

    def concurrent(token):
        with httpx.Client(timeout=45) as client:
            return client.post(base + "/v1/bookings", headers={"Authorization": "Bearer " + token}, json=booking_body("test_a", tomorrow, 6))
    with ThreadPoolExecutor(max_workers=2) as pool:
        replies = list(pool.map(concurrent, [customer, other]))
    for reply in replies:
        if reply.status_code == 201:
            track(reply.json())
    check("two_simultaneous_requests_have_exactly_one_winner", sorted(r.status_code for r in replies) == [201, 409])
    for token, reply in zip([customer, other], replies):
        if reply.status_code == 201:
            request(base, token, "POST", f"/v1/bookings/{reply.json()['id']}/cancel")

    original = request(base, customer, "GET", "/v1/me")
    payload = {key: original.get(key) for key in ["full_name", "phone", "district", "gender", "birthdate"]}
    updated = {**payload, "full_name": "[TEST] Hồ sơ QA", "phone": "0900000099", "birthdate": "2000-02-29", "gender": "Nam"}
    try:
        request(base, customer, "PATCH", "/v1/me", updated)
        persisted = request(base, customer, "GET", "/v1/me")
        auth_user = admin.auth.admin.get_user_by_id(person["id"]).user
        check("contact_phone_birthdate_profile_persist_without_identity_change", persisted["phone"] == updated["phone"] and persisted["birthdate"] == updated["birthdate"] and auth_user.email == original["email"])
        png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a2bQAAAAASUVORK5CYII=")
        with httpx.Client(timeout=45) as client:
            response = client.post(base+"/v1/me/avatar", headers={"Authorization": "Bearer "+customer}, files={"file": ("qa.png", png, "image/png")})
        check("avatar_upload_persists_url", response.status_code == 200 and bool(response.json().get("avatar_url")))
        new_avatar = response.json()["avatar_path"]
        admin.table("profiles").update({"avatar_url": original.get("avatar_url"), "avatar_path": original.get("avatar_path")}).eq("id", person["id"]).execute()
        admin.storage.from_("avatars").remove([new_avatar])
    finally:
        request(base, customer, "PATCH", "/v1/me", payload)
    settings = request(base, customer, "GET", "/v1/me/settings")
    request(base, customer, "PUT", "/v1/me/settings", {"notifications_enabled": False, "profile_public": True})
    saved = request(base, customer, "GET", "/v1/me/settings")
    check("account_settings_persist", not saved["notifications_enabled"] and saved["profile_public"])
    request(base, customer, "PUT", "/v1/me/settings", {"notifications_enabled": settings["notifications_enabled"], "profile_public": settings["profile_public"]})
    request(base, customer, "PUT", "/v1/favorites/test_a?enabled=false", {})
    check("favorite_removal_persists", "test_a" not in request(base, customer, "GET", "/v1/favorites"))
    request(base, customer, "PUT", "/v1/favorites/test_a?enabled=true", {})


def main():
    args = argparse.ArgumentParser()
    args.add_argument("--apply", action="store_true")
    args.add_argument("--base-url", default="http://127.0.0.1:8011")
    options = args.parse_args()
    config = dotenv_values(ROOT / ".env")
    admin = create_client(config["SUPABASE_URL"], config["SUPABASE_SECRET_KEY"])
    people = admin.table("profiles").select("id,email,full_name,role").order("created_at").execute().data
    if not options.apply:
        print(json.dumps({"mode": "preview", "accounts": len(people), "test_pitches": TEST_PITCHES,
                          "days": 8, "slots_per_day": len(RANGES)}, ensure_ascii=False))
        return
    check("correct_local_api", request(options.base_url, None, "GET", "/health")["service"] == "sanbong-api")
    staff_email = "andzen.qa.staff@example.com"
    if not any(p["email"] == staff_email for p in people):
        staff = admin.auth.admin.create_user({"email": staff_email, "password": secrets.token_urlsafe(32),
            "email_confirm": True, "user_metadata": {"full_name": "[TEST] Nhân viên sân"}}).user
        admin.table("profiles").update({"role": "staff"}).eq("id", staff.id).execute()
        people = admin.table("profiles").select("id,email,full_name,role").order("created_at").execute().data
    tokens = {p["id"]: user_token(admin, create_client(config["SUPABASE_URL"], config["SUPABASE_PUBLISHABLE_KEY"]), p["email"]) for p in people}
    admin_token = tokens[next(p["id"] for p in people if p["role"] == "admin")]
    report = {"date": str(TODAY), "accounts": len(people), "checks": checks, "status": "failed"}
    try:
        seed(admin, options.base_url, admin_token, people, tokens)
        integration(admin, options.base_url, admin_token, people, tokens)
        report["status"] = "passed"
    finally:
        for bid in temporary_bookings:
            row = admin.table("bookings").select("status").eq("id", bid).maybe_single().execute().data
            if row and row["status"] == "confirmed":
                request(options.base_url, admin_token, "PATCH", f"/v1/admin/bookings/{bid}", {"status": "cancelled"})
            admin.table("bookings").delete().eq("id", bid).eq("customer_name", "[QA FLOW]").execute()
        output = ROOT.parent / "build" / "qa-booking-report.json"
        output.parent.mkdir(exist_ok=True)
        output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps({"status": report["status"], "checks": len(checks), "report": str(output)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
