package co.za.clevertel.voip.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import co.za.clevertel.voip.utils.SessionManager;

/**
 * WatchdogService
 *
 * A lightweight secondary service whose only job is to restart
 * VoipForegroundService if it is no longer running.
 *
 * Called from:
 *  - VoipForegroundService.onTaskRemoved()  (app swiped away)
 *  - BootReceiver (device rebooted)
 *  - ClevertelApp.onCreate() (cold start)
 *
 * Uses a delayed check so the system has time to fully kill the
 * foreground service before we try to restart it.
 */
public class WatchdogService extends Service {

    private static final String TAG          = "WatchdogService";
    private static final long   DELAY_MS     = 1_500; // wait 1.5 s then restart
    private static final long   CHECK_INTERVAL = 30_000; // re-check every 30 s

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable watchdogTask = new Runnable() {
        @Override
        public void run() {
            SessionManager session = new SessionManager(WatchdogService.this);
            if (session.isLoggedIn()) {
                Log.i(TAG, "Watchdog: (re)starting VoipForegroundService");
                restartVoipService();
            }
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "WatchdogService started");
        handler.postDelayed(watchdogTask, DELAY_MS);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(watchdogTask);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void restartVoipService() {
        try {
            Intent voipIntent = new Intent(this, VoipForegroundService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(voipIntent);
            } else {
                startService(voipIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart VoipForegroundService", e);
        }
    }
}
