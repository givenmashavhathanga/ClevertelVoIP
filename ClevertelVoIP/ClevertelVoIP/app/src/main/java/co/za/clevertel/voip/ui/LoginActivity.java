package co.za.clevertel.voip.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import co.za.clevertel.voip.ClevertelApp;
import co.za.clevertel.voip.R;
import co.za.clevertel.voip.databinding.ActivityLoginBinding;
import co.za.clevertel.voip.utils.SdkEventReceiver;
import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * LoginActivity
 *
 * Entry point for the app. Displays:
 *  - Clevertel logo
 *  - Username field
 *  - Password field
 *  - Login button
 *
 * No SIP domain or server fields — those are hardcoded to sip.clevertel.co.za.
 *
 * Security:
 *  - Input trimmed and validated client-side (server also validates)
 *  - Credentials stored encrypted via SessionManager
 *  - Registration result verified via SDK broadcast before navigating
 */
public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private ActivityLoginBinding binding;
    private SessionManager       sessionManager;
    private boolean              isLoggingIn = false;

    // SDK registration result listener
    private final BroadcastReceiver sdkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String type = intent.getStringExtra(SdkEventReceiver.EXTRA_TYPE);
            if (type == null) return;

            switch (type) {
                case SdkEventReceiver.EVT_REGISTERED:
                    if (isLoggingIn) onRegistrationSuccess();
                    break;
                case SdkEventReceiver.EVT_REG_FAILED:
                    if (isLoggingIn) onRegistrationFailed(
                            intent.getStringExtra(SdkEventReceiver.EXTRA_MESSAGE));
                    break;
                default:
                    break;
            }
        }
    };

    // ── Lifecycle ──────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Skip login screen if already authenticated
        if (sessionManager.isLoggedIn()) {
            navigateToDialler();
            return;
        }

        setupUI();
        requestRequiredPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SdkEventReceiver.registerLocal(this, sdkReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SdkEventReceiver.unregisterLocal(this, sdkReceiver);
    }

    // ── UI Setup ───────────────────────────────────────────────────────

    private void setupUI() {
        // Submit on keyboard "Done" in password field
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    // ── Login logic ────────────────────────────────────────────────────

    private void attemptLogin() {
        if (isLoggingIn) return;

        // Clear previous errors
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);

        String username = binding.etUsername.getText() != null
                ? binding.etUsername.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";

        // Client-side validation
        boolean valid = true;
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.error_username_required));
            valid = false;
        } else if (username.contains(" ")) {
            binding.tilUsername.setError(getString(R.string.error_username_spaces));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (password.length() < 4) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!valid) return;

        setLoadingState(true);
        isLoggingIn = true;

        // Store credentials (encrypted) and trigger SIP registration
        sessionManager.saveSession(username, password);
        VoipConfig.applyGlobalSettings(this);
        VoipConfig.registerAccount(this, username, password);

        // Timeout fallback — if SDK doesn't respond in 15 s, show error
        binding.getRoot().postDelayed(() -> {
            if (isLoggingIn) {
                isLoggingIn = false;
                onRegistrationFailed("Connection timed out. Check your network.");
            }
        }, 15_000);
    }

    private void onRegistrationSuccess() {
        isLoggingIn = false;
        setLoadingState(false);
        ((ClevertelApp) getApplication()).startForegroundServiceSafe();
        navigateToDialler();
    }

    private void onRegistrationFailed(String message) {
        isLoggingIn = false;
        setLoadingState(false);
        sessionManager.clearSession();

        String msg = (message != null && !message.isEmpty())
                ? message : getString(R.string.error_login_failed);
        binding.tilPassword.setError(msg);
    }

    private void setLoadingState(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.etUsername.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setText(loading
                ? R.string.btn_connecting : R.string.btn_login);
    }

    // ── Navigation ─────────────────────────────────────────────────────

    private void navigateToDialler() {
        startActivity(new Intent(this, DiallerActivity.class));
        finish();
    }

    // ── Runtime permissions ────────────────────────────────────────────

    private void requestRequiredPermissions() {
        List<String> needed = new ArrayList<>();

        String[] perms = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // App can function with reduced capability if mic denied; handled in VoipConfig
    }
}
