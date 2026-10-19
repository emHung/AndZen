package com.sanbong.app.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import com.sanbong.app.BuildConfig;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Supabase Auth REST flows used by the Java Android client. */
public final class AuthSession {
    private static final String REDIRECT_URI = "com.sanbong.app://auth-callback";

    private AuthSession() {}

    public static boolean isConfigured() {
        return !BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_PUBLISHABLE_KEY.isEmpty();
    }

    public static boolean hasSession(Context context) {
        return SecureSessionStore.load(context) != null;
    }

    public static JSONObject sendPhoneOtp(String phone, boolean createUser, String fullName) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("phone", normalizePhone(phone));
        payload.put("create_user", createUser);
        if (fullName != null && !fullName.trim().isEmpty()) {
            payload.put("data", new JSONObject().put("full_name", fullName.trim()));
        }
        return request("POST", "/auth/v1/otp", payload, null);
    }

    public static JSONObject verifyPhoneOtp(Context context, String phone, String token) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("phone", normalizePhone(phone));
        payload.put("token", token.trim());
        payload.put("type", "sms");
        JSONObject response = request("POST", "/auth/v1/verify", payload, null);
        saveSession(context, response);
        return response;
    }

    public static JSONObject signInWithEmail(Context context, String email, String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email.trim());
        payload.put("password", password);
        JSONObject response = request("POST", "/auth/v1/token?grant_type=password", payload, null);
        saveSession(context, response);
        return response;
    }

    public static JSONObject signUpWithEmail(Context context, String email, String password,
                                             String fullName, String phone) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email.trim());
        payload.put("password", password);
        JSONObject metadata = new JSONObject().put("full_name", fullName.trim());
        if (phone != null && !phone.trim().isEmpty()) metadata.put("phone", normalizePhone(phone));
        payload.put("data", metadata);
        JSONObject response = request("POST", "/auth/v1/signup", payload, null);
        if (response.has("access_token")) saveSession(context, response);
        return response;
    }

    public static JSONObject requestPasswordReset(String email) throws Exception {
        return request("POST", "/auth/v1/recover", new JSONObject().put("email", email.trim()), null);
    }

    public static Intent googleSignInIntent(Context context) throws Exception {
        byte[] random = new byte[48];
        new SecureRandom().nextBytes(random);
        String verifier = Base64.encodeToString(random, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        String challenge = Base64.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        context.getSharedPreferences("auth_oauth", Context.MODE_PRIVATE).edit().putString("pkce_verifier", verifier).apply();
        String url = BuildConfig.SUPABASE_URL + "/auth/v1/authorize?provider=google"
                + "&apikey=" + URLEncoder.encode(BuildConfig.SUPABASE_PUBLISHABLE_KEY, "UTF-8")
                + "&redirect_to=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&code_challenge=" + URLEncoder.encode(challenge, "UTF-8")
                + "&code_challenge_method=s256";
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public static JSONObject completeGoogleCallback(Context context, Uri callback) throws Exception {
        String tokenHash = callback.getQueryParameter("token_hash");
        String verificationType = callback.getQueryParameter("type");
        if (tokenHash != null && !tokenHash.isEmpty() && verificationType != null) {
            JSONObject verification = new JSONObject().put("token_hash", tokenHash).put("type", verificationType);
            JSONObject verified = request("POST", "/auth/v1/verify", verification, null);
            if (verified.has("access_token")) saveSession(context, verified);
            return verified;
        }
        String code = callback.getQueryParameter("code");
        if (code == null || code.isEmpty()) {
            throw new IllegalStateException("Không thể hoàn tất đăng nhập Google. Hãy thử lại hoặc chọn cách đăng nhập khác.");
        }
        String verifier = context.getSharedPreferences("auth_oauth", Context.MODE_PRIVATE)
                .getString("pkce_verifier", null);
        if (verifier == null) throw new IllegalStateException("Phiên Google đã hết hạn. Hãy thử lại.");
        JSONObject payload = new JSONObject().put("auth_code", code).put("code_verifier", verifier);
        JSONObject response = request("POST", "/auth/v1/token?grant_type=pkce", payload, null);
        context.getSharedPreferences("auth_oauth", Context.MODE_PRIVATE).edit().clear().apply();
        saveSession(context, response);
        return response;
    }

    public static void signOut(Context context) throws Exception {
        JSONObject session = SecureSessionStore.load(context);
        if (session != null) request("POST", "/auth/v1/logout", new JSONObject(), session.optString("access_token"));
        SecureSessionStore.clear(context);
    }

    public static JSONObject updatePassword(String password, Context context) throws Exception {
        JSONObject session = SecureSessionStore.load(context);
        if (session == null) throw new IllegalStateException("Vui lòng đăng nhập lại.");
        return request("PUT", "/auth/v1/user", new JSONObject().put("password", password), accessToken(context));
    }

    public static synchronized String accessToken(Context context) throws Exception {
        JSONObject session = SecureSessionStore.load(context);
        if (session == null) return null;
        long expiresAt = session.optLong("expires_at", 0);
        if (expiresAt > System.currentTimeMillis() / 1000L + 60) return session.optString("access_token", null);
        String refresh = session.optString("refresh_token", "");
        if (refresh.isEmpty()) {
            SecureSessionStore.clear(context);
            return null;
        }
        JSONObject response = request("POST", "/auth/v1/token?grant_type=refresh_token",
                new JSONObject().put("refresh_token", refresh), null);
        saveSession(context, response);
        return response.optString("access_token", null);
    }

    private static void saveSession(Context context, JSONObject response) throws Exception {
        if (!response.has("access_token") || !response.has("refresh_token")) {
            throw new IllegalStateException("Supabase chưa tạo phiên đăng nhập. Hãy kiểm tra cấu hình xác minh tài khoản.");
        }
        long expiresAt = response.optLong("expires_at", 0);
        if (expiresAt == 0) expiresAt = System.currentTimeMillis() / 1000L + response.optLong("expires_in", 3600);
        response.put("expires_at", expiresAt);
        SecureSessionStore.save(context, response);
    }

    private static JSONObject request(String method, String path, JSONObject payload, String token) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("Thiếu SUPABASE_URL hoặc SUPABASE_PUBLISHABLE_KEY trong local.properties.");
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.SUPABASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        if (payload != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = connection.getResponseCode();
        InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream();
        String body = readAll(input);
        connection.disconnect();
        if (status >= 400) {
            String message;
            if (status == 400 || status == 401) {
                message = "Thông tin đăng nhập chưa đúng hoặc phiên đăng nhập đã hết hạn.";
            } else if (status == 403) {
                message = "Tài khoản chưa được phép thực hiện thao tác này.";
            } else if (status == 422) {
                message = "Thông tin chưa hợp lệ hoặc tài khoản đã tồn tại.";
            } else if (status == 429) {
                message = "Bạn thao tác quá nhanh. Hãy chờ một lúc rồi thử lại.";
            } else if (status >= 500) {
                message = "Dịch vụ xác thực đang bận. Vui lòng thử lại sau.";
            } else {
                message = "Không thể hoàn tất yêu cầu xác thực. Vui lòng thử lại.";
            }
            throw new IllegalStateException(message);
        }
        return body.trim().isEmpty() ? new JSONObject() : new JSONObject(body);
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString("UTF-8");
        }
    }

    private static String normalizePhone(String phone) {
        String normalized = phone.replaceAll("[\\s().-]", "");
        if (normalized.startsWith("0")) return "+84" + normalized.substring(1);
        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }
}
