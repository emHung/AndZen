import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import httpx
from app.main import app
from app.security import CurrentUser, require_user
from app.supabase_client import read_query
from fastapi import HTTPException


ADMIN = CurrentUser("00000000-0000-0000-0000-000000000001", "admin@example.com", None, "admin")
BOOKING = "00000000-0000-0000-0000-000000000010"


class ContractsTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        app.dependency_overrides[require_user] = lambda: ADMIN
        self.api = httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://qa.local")

    async def asyncTearDown(self):
        await self.api.aclose()
        app.dependency_overrides.clear()

    @patch("app.routers.admin.get_supabase")
    async def test_admin_cancellation_uses_one_transactional_rpc(self, factory):
        client = factory.return_value
        client.rpc.return_value.execute.return_value.data = [{"id": BOOKING, "status": "cancelled"}]
        reply = await self.api.patch(f"/v1/admin/bookings/{BOOKING}", json={"status": "cancelled"})
        self.assertEqual(reply.status_code, 200)
        client.rpc.assert_called_once_with("admin_booking_status", {
            "p_actor_id": ADMIN.id, "p_booking_id": BOOKING, "p_status": "cancelled"})
        client.table.assert_not_called()

    @patch("app.routers.admin.managed_pitch_ids", return_value=[])
    @patch("app.routers.admin.get_supabase")
    async def test_unassigned_staff_gets_no_global_booking_data(self, factory, scopes):
        app.dependency_overrides[require_user] = lambda: CurrentUser(ADMIN.id, ADMIN.email, None, "staff")
        reply = await self.api.get("/v1/admin/bookings")
        self.assertEqual(reply.status_code, 200)
        self.assertEqual(reply.json(), [])
        factory.assert_not_called()

    async def test_customer_cannot_manage_or_assign_staff(self):
        app.dependency_overrides[require_user] = lambda: CurrentUser(ADMIN.id, ADMIN.email, None, "customer")
        self.assertEqual((await self.api.get("/v1/admin/pitches")).status_code, 403)
        self.assertEqual((await self.api.get("/v1/admin/staff")).status_code, 403)

    @patch("app.routers.account.get_supabase")
    async def test_profile_email_change_cannot_partially_write_profile(self, factory):
        client = factory.return_value
        client.auth.admin.get_user_by_id.return_value.user = SimpleNamespace(email=ADMIN.email)
        reply = await self.api.patch("/v1/me", json={"full_name": "Example User", "email": "new@example.com"})
        self.assertEqual(reply.status_code, 422)
        client.table.assert_not_called()

    @patch("app.routers.account.httpx.put")
    @patch("app.routers.account.get_supabase")
    async def test_pending_email_keeps_existing_identity_and_uses_user_token(self, factory, request):
        factory.return_value.auth.admin.get_user_by_id.return_value.user = SimpleNamespace(email=ADMIN.email)
        request.return_value.status_code = 200
        request.return_value.json.return_value = {"email": ADMIN.email}
        reply = await self.api.post("/v1/me/email", json={"email": "new@example.com"}, headers={"Authorization": "Bearer test-user-token"})
        self.assertEqual(reply.status_code, 200)
        self.assertTrue(reply.json()["email_change_pending"])
        self.assertEqual(request.call_args.kwargs["headers"]["Authorization"], "Bearer test-user-token")
        factory.return_value.table.assert_not_called()

    async def test_role_injection_is_rejected(self):
        self.assertEqual((await self.api.patch("/v1/me", json={"full_name": "Example User", "role": "admin"})).status_code, 422)

    @patch("app.routers.admin.require_pitch_access")
    @patch("app.routers.admin.require_bookable_date")
    async def test_slot_end_with_seconds_cannot_equal_start(self, window, access):
        reply = await self.api.post("/v1/admin/pitches/test/slots", json={"slot_date": "2026-10-16",
                          "starts_at": "06:00", "ends_at": "06:00:00", "price": 100000})
        self.assertEqual(reply.status_code, 422)


class SupabaseReadRetryTest(unittest.TestCase):
    @patch("app.supabase_client.get_settings", return_value=SimpleNamespace(supabase_read_retries=2, supabase_retry_delay_seconds=0))
    def test_transient_read_failure_is_retried(self, settings):
        query = MagicMock()
        query.execute.side_effect = [httpx.ReadError("temporary"), SimpleNamespace(data=[{"id": "test"}])]
        self.assertEqual(read_query(query).data, [{"id": "test"}])
        self.assertEqual(query.execute.call_count, 2)

    @patch("app.supabase_client.get_settings", return_value=SimpleNamespace(supabase_read_retries=2, supabase_retry_delay_seconds=0))
    def test_retry_is_bounded_and_reports_service_unavailable(self, settings):
        query = MagicMock()
        query.execute.side_effect = httpx.ReadError("temporary")
        with self.assertRaises(HTTPException) as error:
            read_query(query)
        self.assertEqual(error.exception.status_code, 503)
        self.assertEqual(query.execute.call_count, 3)


if __name__ == "__main__":
    unittest.main()
