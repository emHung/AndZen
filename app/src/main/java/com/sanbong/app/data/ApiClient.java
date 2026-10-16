package com.sanbong.app.data;

import android.content.Context;

import com.sanbong.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Small blocking HTTP client. Call it only from a worker thread. */
public final class ApiClient {
    private ApiClient() {}

    public static JSONObject get(Context context, String path) throws Exception {
        return request(context, "GET", path, null);
    }

    public static JSONObject post(Context context, String path, JSONObject body) throws Exception {
        return request(context, "POST", path, body);
    }

    public static JSONObject put(Context context, String path, JSONObject body) throws Exception {
        return request(context, "PUT", path, body);
    }

    public static JSONObject patch(Context context, String path, JSONObject body) throws Exception {
        return request(context, "PATCH", path, body);
    }

    public static JSONObject delete(Context context, String path) throws Exception {
        return request(context, "DELETE", path, null);
    }

    public static JSONArray getArray(Context context, String path) throws Exception {
        String response = execute(context, "GET", path, null, null);
        return new JSONArray(response);
    }

    public static JSONArray getPublicArray(String path) throws Exception {
        return new JSONArray(executePublic("GET", path));
    }

    private static String executePublic(String method, String path) throws Exception {
        String base = BuildConfig.API_BASE_URL;
        if (base == null || base.trim().isEmpty()) throw new IllegalStateException("Chưa cấu hình API_BASE_URL.");
        HttpURLConnection connection = (HttpURLConnection) new URL(base.replaceAll("/+$", "") + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        int status = connection.getResponseCode();
        InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();
        if (status >= 400) throw new IllegalStateException("Máy chủ trả lỗi " + status + ".");
        return response;
    }

    public static JSONObject uploadAvatar(Context context, File file, String mimeType) throws Exception {
        String boundary = "SanBong-" + System.currentTimeMillis();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"avatar\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) body.write(buffer, 0, count);
        }
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return new JSONObject(execute(context, "POST", "/v1/me/avatar", body.toByteArray(),
                "multipart/form-data; boundary=" + boundary));
    }

    private static JSONObject request(Context context, String method, String path, JSONObject payload) throws Exception {
        byte[] body = payload == null ? null : payload.toString().getBytes(StandardCharsets.UTF_8);
        String response = execute(context, method, path, body, "application/json");
        return response.trim().isEmpty() ? new JSONObject() : new JSONObject(response);
    }

    private static String execute(Context context, String method, String path, byte[] body,
                                  String contentType) throws Exception {
        String base = BuildConfig.API_BASE_URL;
        if (base == null || base.trim().isEmpty()) throw new IllegalStateException("Chưa cấu hình API_BASE_URL.");
        String token = AuthSession.accessToken(context);
        if (token == null || token.isEmpty()) throw new IllegalStateException("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.");
        HttpURLConnection connection = (HttpURLConnection) new URL(base.replaceAll("/+$", "") + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "application/json");
        if (contentType != null) connection.setRequestProperty("Content-Type", contentType);
        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) { output.write(body); }
        }
        int status = connection.getResponseCode();
        InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();
        if (status >= 400) {
            String message = "Máy chủ trả lỗi " + status + ".";
            try { message = new JSONObject(response).optString("detail", message); } catch (Exception ignoredError) { }
            throw new IllegalStateException(message);
        }
        return response;
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        }
    }
}
