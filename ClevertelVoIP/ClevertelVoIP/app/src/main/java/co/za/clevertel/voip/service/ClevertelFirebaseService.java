package co.za.clevertel.voip.service;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * ClevertelFirebaseService
 *
 * Stub push notification service.
 * To enable full FCM push: add Firebase to the project (google-services.json)
 * and extend FirebaseMessagingService instead.
 *
 * For now, push wakeup is handled via MizuVoIP's built-in FCM gateway
 * configured through VoipConfig.applyGlobalSettings() (pushtype=fcm_gateway).
 */
public class ClevertelFirebaseService extends Service {

    private static final String TAG = "ClevertelFCM";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Push service started — waking SIP stack");

        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            VoipConfig.applyGlobalSettings(this);
            VoipConfig.registerAccount(this, session.getUsername(), session.getPassword());

            Intent voipIntent = new Intent(this, VoipForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(voipIntent);
            } else {
                startService(voipIntent);
            }
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
