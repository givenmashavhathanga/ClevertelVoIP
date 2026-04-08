package co.za.clevertel.voip.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import co.za.clevertel.voip.R;
import co.za.clevertel.voip.databinding.ActivityIncallBinding;
import co.za.clevertel.voip.utils.SdkEventReceiver;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * InCallActivity
 *
 * Displayed during an active or incoming call.
 *  - Incoming: shows caller ID, Accept / Reject buttons
 *  - Active  : shows call timer, Mute / Hold / Hangup / Dialpad (DTMF) buttons
 *
 * Shows on lock screen (showOnLockScreen / turnScreenOn in manifest).
 */
public class InCallActivity extends AppCompatActivity {

    public static final String EXTRA_CALLER_ID = "caller_id";
    public static final String EXTRA_OUTBOUND  = "outbound";

    private ActivityIncallBinding binding;
    private boolean isMuted  = false;
    private boolean isOnHold = false;
    private boolean isOutbound;

    // Call timer
    private final Handler  timerHandler  = new Handler(Looper.getMainLooper());
    private       long     callStartMs   = 0;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (callStartMs == 0) return;
            long elapsed = (System.currentTimeMillis() - callStartMs) / 1000;
            long mm = elapsed / 60;
            long ss = elapsed % 60;
            binding.tvCallTimer.setText(String.format("%02d:%02d", mm, ss));
            timerHandler.postDelayed(this, 1000);
        }
    };

    // ── SDK events ─────────────────────────────────────────────────────
    private final BroadcastReceiver sdkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String type = intent.getStringExtra(SdkEventReceiver.EXTRA_TYPE);
            if (type == null) return;

            switch (type) {
                case SdkEventReceiver.EVT_CONNECTED:
                    onCallConnected();
                    break;
                case SdkEventReceiver.EVT_DISCONNECTED:
                    onCallEnded();
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

        // Keep screen on and show over lock screen during call
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        binding = ActivityIncallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String callerId = getIntent().getStringExtra(EXTRA_CALLER_ID);
        isOutbound      = getIntent().getBooleanExtra(EXTRA_OUTBOUND, false);

        binding.tvCallerName.setText(
                (callerId != null && !callerId.isEmpty()) ? callerId : getString(R.string.unknown_caller));

        setupButtons();

        if (isOutbound) {
            showOutboundUI();
        } else {
            showIncomingUI();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onBackPressed() {
        // Prevent accidental back-press ending call
    }

    // ── UI states ──────────────────────────────────────────────────────

    private void showIncomingUI() {
        binding.layoutIncoming.setVisibility(android.view.View.VISIBLE);
        binding.layoutActive.setVisibility(android.view.View.GONE);
        binding.tvCallStatus.setText(R.string.status_incoming);
    }

    private void showOutboundUI() {
        binding.layoutIncoming.setVisibility(android.view.View.GONE);
        binding.layoutActive.setVisibility(android.view.View.VISIBLE);
        binding.tvCallStatus.setText(R.string.status_calling);
    }

    private void onCallConnected() {
        runOnUiThread(() -> {
            binding.layoutIncoming.setVisibility(android.view.View.GONE);
            binding.layoutActive.setVisibility(android.view.View.VISIBLE);
            binding.tvCallStatus.setText(R.string.status_connected);
            callStartMs = System.currentTimeMillis();
            timerHandler.post(timerRunnable);
        });
    }

    private void onCallEnded() {
        runOnUiThread(() -> {
            timerHandler.removeCallbacks(timerRunnable);
            finish();
        });
    }

    // ── Button setup ───────────────────────────────────────────────────

    private void setupButtons() {
        // Incoming call actions
        binding.btnAccept.setOnClickListener(v -> {
            VoipConfig.accept(this);
            onCallConnected();
        });

        binding.btnReject.setOnClickListener(v -> {
            VoipConfig.reject(this);
            finish();
        });

        // Active call actions
        binding.btnHangup.setOnClickListener(v -> {
            VoipConfig.hangup(this);
            finish();
        });

        binding.btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            VoipConfig.toggleMute(this, isMuted);
            binding.btnMute.setSelected(isMuted);
            binding.btnMute.setAlpha(isMuted ? 0.5f : 1.0f);
            binding.tvMuteLabel.setText(isMuted ? R.string.unmute : R.string.mute);
        });

        binding.btnHold.setOnClickListener(v -> {
            isOnHold = !isOnHold;
            VoipConfig.toggleHold(this, isOnHold);
            binding.btnHold.setSelected(isOnHold);
            binding.btnHold.setAlpha(isOnHold ? 0.5f : 1.0f);
            binding.tvHoldLabel.setText(isOnHold ? R.string.resume : R.string.hold);
            if (isOnHold) timerHandler.removeCallbacks(timerRunnable);
            else {
                callStartMs = System.currentTimeMillis();
                timerHandler.post(timerRunnable);
            }
        });

        // DTMF keypad buttons (visible on dialpad expand)
        int[] dtmfIds = { R.id.dtmf_0,R.id.dtmf_1,R.id.dtmf_2,R.id.dtmf_3,
                          R.id.dtmf_4,R.id.dtmf_5,R.id.dtmf_6,R.id.dtmf_7,
                          R.id.dtmf_8,R.id.dtmf_9,R.id.dtmf_star,R.id.dtmf_hash };
        String[] dtmfDigits = {"0","1","2","3","4","5","6","7","8","9","*","#"};

        for (int i = 0; i < dtmfIds.length; i++) {
            final String digit = dtmfDigits[i];
            android.view.View btn = binding.getRoot().findViewById(dtmfIds[i]);
            if (btn != null) btn.setOnClickListener(v -> VoipConfig.sendDtmf(this, digit));
        }

        // Dialpad toggle
        if (binding.btnDialpad != null) {
            binding.btnDialpad.setOnClickListener(v -> {
                boolean visible = binding.layoutDtmf.getVisibility()
                        == android.view.View.VISIBLE;
                binding.layoutDtmf.setVisibility(visible
                        ? android.view.View.GONE : android.view.View.VISIBLE);
            });
        }
    }
}
