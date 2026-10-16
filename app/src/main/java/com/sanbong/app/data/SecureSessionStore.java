package com.sanbong.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores Supabase sessions encrypted with an Android Keystore key. */
public final class SecureSessionStore {
    private static final String PREFS = "secure_auth_session";
    private static final String VALUE = "session_v1";
    private static final String KEY_ALIAS = "sanbong.session.key";

    private SecureSessionStore() {}

    public static synchronized void save(Context context, JSONObject session) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(session.toString().getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        prefs(context).edit().putString(VALUE, Base64.encodeToString(payload, Base64.NO_WRAP)).apply();
    }

    public static synchronized JSONObject load(Context context) {
        String encoded = prefs(context).getString(VALUE, null);
        if (encoded == null) return null;
        try {
            byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
            int ivLength = 12;
            byte[] iv = new byte[ivLength];
            byte[] encrypted = new byte[payload.length - ivLength];
            System.arraycopy(payload, 0, iv, 0, ivLength);
            System.arraycopy(payload, ivLength, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new JSONObject(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            clear(context);
            return null;
        }
    }

    public static synchronized void clear(Context context) {
        prefs(context).edit().remove(VALUE).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
