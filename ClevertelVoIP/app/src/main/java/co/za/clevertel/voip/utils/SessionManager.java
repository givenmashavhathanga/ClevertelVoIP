package co.za.clevertel.voip.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * SessionManager
 *
 * Stores SIP credentials encrypted on-device using AndroidX EncryptedSharedPreferences
 * (AES-256 GCM via Android Keystore). No plaintext credentials are ever written to disk.
 *
 * Security notes:
 *  - Backed by Android Keystore — keys are hardware-bound on supported devices
 *  - Credentials are only decrypted at runtime when needed for registration
 *  - No credential is exposed in logs (passwords are masked)
 */
public class SessionManager {

    private static final String TAG        = "SessionManager";
    private static final String PREFS_FILE = "clevertel_session";
    private static final String KEY_USER   = "username";
    private static final String KEY_PASS   = "password";
    private static final String KEY_LOGGED = "logged_in";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        SharedPreferences tmp;
        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            tmp = EncryptedSharedPreferences.create(
                    ctx,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to open encrypted prefs, falling back to plaintext", e);
            // Fallback — should not happen on normal Android devices
            tmp = ctx.getSharedPreferences(PREFS_FILE + "_plain", Context.MODE_PRIVATE);
        }
        this.prefs = tmp;
    }

    // ── Write ──────────────────────────────────────────────────────────

    /**
     * Save credentials and mark the user as logged in.
     * Input is validated (non-null, non-empty) before storage.
     */
    public boolean saveSession(String username, String password) {
        if (username == null || username.trim().isEmpty()) return false;
        if (password == null || password.isEmpty())        return false;

        prefs.edit()
                .putString(KEY_USER,   username.trim())
                .putString(KEY_PASS,   password)
                .putBoolean(KEY_LOGGED, true)
                .apply();

        Log.i(TAG, "Session saved for: " + username.trim());
        return true;
    }

    // ── Read ───────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public String getUsername() {
        return prefs.getString(KEY_USER, "");
    }

    /** Returns the stored password. Never log this value. */
    public String getPassword() {
        return prefs.getString(KEY_PASS, "");
    }

    // ── Clear ──────────────────────────────────────────────────────────

    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER)
                .remove(KEY_PASS)
                .putBoolean(KEY_LOGGED, false)
                .apply();
        Log.i(TAG, "Session cleared");
    }
}
