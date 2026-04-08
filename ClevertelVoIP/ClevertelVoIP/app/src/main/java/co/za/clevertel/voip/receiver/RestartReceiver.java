package co.za.clevertel.voip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import co.za.clevertel.voip.service.VoipForegroundService;
import co.za.clevertel.voip.service.WatchdogService;

/**
 * RestartReceiver
 *
 * Receives an internal broadcast sent by VoipForegroundService.onTaskRemoved()
 * and WatchdogService to trigger a clean restart of the VoIP service.
 *
 * This is part of the auto-restart chain:
 *  app killed → onTaskRemoved → starts WatchdogService
 *               WatchdogService → starts VoipForegroundService
 *  OS kill     → START_STICKY → OS restarts VoipForegroundService
 *  boot        → BootReceiver → starts everything fresh
 */
public class RestartReceiver extends BroadcastReceiver {

    private static final String TAG = "RestartReceiver";

    public static final String ACTION_RESTART = "co.za.clevertel.voip.RESTART";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.i(TAG, "Restart broadcast received — relaunching services");

        Intent voip = new Intent(ctx, VoipForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(voip);
        } else {
            ctx.startService(voip);
        }

        ctx.startService(new Intent(ctx, WatchdogService.class));
    }
}
