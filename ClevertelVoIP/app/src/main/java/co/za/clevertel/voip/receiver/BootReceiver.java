package co.za.clevertel.voip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import co.za.clevertel.voip.service.VoipForegroundService;
import co.za.clevertel.voip.service.WatchdogService;
import co.za.clevertel.voip.utils.SessionManager;
import co.za.clevertel.voip.utils.VoipConfig;

/**
 * BootReceiver
 *
 * Listens for BOOT_COMPLETED (and vendor equivalents) so the VoIP
 * stack auto-starts after a device reboot without requiring the user
 * to open the app first.
 *
 * Maps to SDK setting: autostart=2 / startonboot=true
 *
 * Only starts services if the user has a valid saved session — the app
 * will not auto-register on behalf of a logged-out user.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (!action.equals(Intent.ACTION_BOOT_COMPLETED)
                && !action.equals("android.intent.action.QUICKBOOT_POWERON")
                && !action.equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
            return;
        }

        Log.i(TAG, "Boot completed — checking for saved session");

        SessionManager session = new SessionManager(ctx);
        if (!session.isLoggedIn()) {
            Log.i(TAG, "No saved session, skipping auto-start");
            return;
        }

        Log.i(TAG, "Auto-starting VoIP stack for: " + session.getUsername());

        // Apply SIP config and re-register
        VoipConfig.applyGlobalSettings(ctx);
        VoipConfig.registerAccount(ctx, session.getUsername(), session.getPassword());

        // Start foreground service
        Intent voip = new Intent(ctx, VoipForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(voip);
        } else {
            ctx.startService(voip);
        }

        // Start watchdog
        ctx.startService(new Intent(ctx, WatchdogService.class));
    }
}
