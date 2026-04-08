package co.za.clevertel.voip;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import co.za.clevertel.voip.service.VoipForegroundService;
import co.za.clevertel.voip.service.WatchdogService;
import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * ClevertelApp — Application entry point.
 *
 * Responsibilities:
 *  • Create notification channels (required Android 8+)
 *  • Apply hardcoded VoIP configuration (SIP domain, wifi, push, background)
 *  • Re-register the SIP account if credentials exist (auto-restart)
 *  • Start the foreground keep-alive service
 */
public class ClevertelApp extends Application {

    public static final String CHANNEL_CALL   = "clevertel_call";
    public static final String CHANNEL_SILENT = "clevertel_silent";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();
        VoipConfig.applyGlobalSettings(this);

        // If the user was previously logged in, re-register and keep alive
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            VoipConfig.registerAccount(this,
                    session.getUsername(),
                    session.getPassword());
            startForegroundServiceSafe();
        }
    }

    // ── Notification channels ──────────────────────────────────────────

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);

        // High-importance channel for incoming calls
        NotificationChannel call = new NotificationChannel(
                CHANNEL_CALL,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH);
        call.setDescription("Clevertel VoIP incoming call alerts");
        call.enableVibration(true);
        call.enableLights(true);
        nm.createNotificationChannel(call);

        // Low-importance channel for the persistent keep-alive notification
        NotificationChannel silent = new NotificationChannel(
                CHANNEL_SILENT,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW);
        silent.setDescription("Keeps the Clevertel VoIP service running");
        nm.createNotificationChannel(silent);
    }

    // ── Service helpers ────────────────────────────────────────────────

    public void startForegroundServiceSafe() {
        Intent intent = new Intent(this, VoipForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // Also start the watchdog so we auto-restart if killed
        startService(new Intent(this, WatchdogService.class));
    }
}
