package co.za.clevertel.voip.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import co.za.clevertel.voip.ClevertelApp;
import co.za.clevertel.voip.R;
import co.za.clevertel.voip.ui.DiallerActivity;
import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * VoipForegroundService
 *
 * Runs as an Android foreground service (phoneCall type) to:
 *  - Keep the SIP stack registered while the app is not in the foreground
 *  - Prevent the OS from killing the VoIP engine
 *  - Receive incoming calls in the background
 *
 * The persistent notification tells the user the app is active and
 * lets them tap back into the dialler.
 *
 * "Run in background = yes / foreground = foreign" translates to this
 * service pattern: started at login, stopped only on explicit logout.
 *
 * Restart behaviour:
 *  - START_STICKY ensures the OS restarts this service if killed
 *  - WatchdogService additionally monitors and restarts if needed
 *  - BootReceiver starts this on device boot
 */
public class VoipForegroundService extends Service {

    private static final int NOTIFICATION_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());

        // Re-apply settings and re-register in case this is a restart
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            VoipConfig.applyGlobalSettings(this);
            VoipConfig.registerAccount(this, session.getUsername(), session.getPassword());
        }

        // START_STICKY: OS will restart the service if killed, passing null intent
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App was swiped away — schedule a restart via WatchdogService
        Intent restart = new Intent(this, WatchdogService.class);
        startService(restart);
        super.onTaskRemoved(rootIntent);
    }

    // ── Notification ───────────────────────────────────────────────────

    private Notification buildNotification() {
        PendingIntent openApp = PendingIntent.getActivity(
                this, 0,
                new Intent(this, DiallerActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, ClevertelApp.CHANNEL_SILENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_running))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
