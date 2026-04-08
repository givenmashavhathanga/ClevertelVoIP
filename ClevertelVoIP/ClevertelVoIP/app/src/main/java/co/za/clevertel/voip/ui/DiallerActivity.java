package co.za.clevertel.voip.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import co.za.clevertel.voip.R;
import co.za.clevertel.voip.databinding.ActivityDiallerBinding;
import co.za.clevertel.voip.utils.SdkEventReceiver;
import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * DiallerActivity
 *
 * Main screen shown after successful login. Features:
 *  - SIP registration status indicator
 *  - Numeric dialpad (0–9, *, #)
 *  - Backspace
 *  - Call button → navigates to InCallActivity
 *  - Incoming call detection → launches InCallActivity
 *  - Logout
 */
public class DiallerActivity extends AppCompatActivity {

    private ActivityDiallerBinding binding;
    private SessionManager         sessionManager;
    private final StringBuilder    dialBuffer = new StringBuilder();

    // ── SDK event listener ─────────────────────────────────────────────
    private final BroadcastReceiver sdkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String type     = intent.getStringExtra(SdkEventReceiver.EXTRA_TYPE);
            String callerId = intent.getStringExtra(SdkEventReceiver.EXTRA_CALLERID);
            if (type == null) return;

            switch (type) {
                case SdkEventReceiver.EVT_REGISTERED:
                    setStatus(true, getString(R.string.status_registered));
                    break;
                case SdkEventReceiver.EVT_UNREGISTERED:
                case SdkEventReceiver.EVT_REG_FAILED:
                    setStatus(false, getString(R.string.status_not_registered));
                    break;
                case SdkEventReceiver.EVT_RINGING:
                    // Incoming call — launch InCallActivity
                    openInCall(callerId, false);
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
        binding = ActivityDiallerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setupDialpad();
        setupToolbar();

        // Refresh registration status on open
        VoipConfig.requestStatus(this);
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

    // ── Dialpad setup ──────────────────────────────────────────────────

    private void setupDialpad() {
        int[] digitBtnIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9, R.id.btn_star, R.id.btn_hash
        };
        String[] digits = {"0","1","2","3","4","5","6","7","8","9","*","#"};

        for (int i = 0; i < digitBtnIds.length; i++) {
            final String digit = digits[i];
            binding.getRoot().findViewById(digitBtnIds[i]).setOnClickListener(v -> appendDigit(digit));
        }

        binding.btnBackspace.setOnClickListener(v -> {
            if (dialBuffer.length() > 0) {
                dialBuffer.deleteCharAt(dialBuffer.length() - 1);
                binding.etDialNumber.setText(dialBuffer.toString());
            }
        });

        binding.btnBackspace.setOnLongClickListener(v -> {
            dialBuffer.setLength(0);
            binding.etDialNumber.setText("");
            return true;
        });

        binding.btnCall.setOnClickListener(v -> {
            String number = binding.etDialNumber.getText() != null
                    ? binding.etDialNumber.getText().toString().trim()
                    : dialBuffer.toString().trim();
            if (!number.isEmpty()) {
                VoipConfig.call(this, number);
                openInCall(number, true);
            }
        });
    }

    private void appendDigit(String digit) {
        dialBuffer.append(digit);
        binding.etDialNumber.setText(dialBuffer.toString());
    }

    // ── Toolbar / logout ───────────────────────────────────────────────

    private void setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            }
            return false;
        });
    }

    private void logout() {
        VoipConfig.stopStack(this);
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // ── Status indicator ───────────────────────────────────────────────

    private void setStatus(boolean registered, String label) {
        runOnUiThread(() -> {
            binding.tvStatus.setText(label);
            binding.statusDot.setBackgroundResource(
                    registered ? R.drawable.dot_green : R.drawable.dot_red);
        });
    }

    // ── Navigation ─────────────────────────────────────────────────────

    private void openInCall(String callerId, boolean outbound) {
        Intent intent = new Intent(this, InCallActivity.class);
        intent.putExtra(InCallActivity.EXTRA_CALLER_ID, callerId);
        intent.putExtra(InCallActivity.EXTRA_OUTBOUND,  outbound);
        startActivity(intent);
    }
}
