package co.za.clevertel.voip.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * SdkEventReceiver
 *
 * Listens to MizuVoIP SDK broadcast events and re-broadcasts them
 * locally so Activities and Services can respond without coupling
 * to system-level broadcasts.
 *
 * SDK sends events via the global broadcast action; we convert to
 * LocalBroadcastManager events for security (no cross-app exposure).
 *
 * Usage in an Activity:
 *   SdkEventReceiver.registerLocal(this, receiver);
 *   // in receiver.onReceive(): read extras "event_type", "status", "callerid"
 */
public class SdkEventReceiver extends BroadcastReceiver {

    private static final String TAG = "SdkEventReceiver";

    // Actions this receiver listens for (from SDK)
    public static final String SDK_ACTION     = "co.za.clevertel.voip.JVOIP_EVENT";

    // Local re-broadcast actions (used between app components)
    public static final String LOCAL_ACTION   = "co.za.clevertel.voip.LOCAL_EVENT";
    public static final String EXTRA_TYPE     = "event_type";
    public static final String EXTRA_STATUS   = "status";
    public static final String EXTRA_CALLERID = "callerid";
    public static final String EXTRA_MESSAGE  = "message";
    public static final String EXTRA_LINE     = "line";

    // Event type constants
    public static final String EVT_RINGING        = "ringing";
    public static final String EVT_CONNECTED      = "connected";
    public static final String EVT_DISCONNECTED   = "disconnected";
    public static final String EVT_REGISTERED     = "registered";
    public static final String EVT_UNREGISTERED   = "unregistered";
    public static final String EVT_REG_FAILED     = "reg_failed";
    public static final String EVT_STATUS         = "status";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;

        String function = intent.getStringExtra("function");
        String status   = intent.getStringExtra("status");
        String callerid = intent.getStringExtra("callerid");
        String message  = intent.getStringExtra("message");
        int    line     = intent.getIntExtra("line", 0);

        Log.d(TAG, "SDK event → function=" + function + " status=" + status);

        // Translate SDK events to local events
        String eventType = translateEvent(function, status);

        Intent local = new Intent(LOCAL_ACTION);
        local.putExtra(EXTRA_TYPE,     eventType);
        local.putExtra(EXTRA_STATUS,   status   != null ? status   : "");
        local.putExtra(EXTRA_CALLERID, callerid != null ? callerid : "");
        local.putExtra(EXTRA_MESSAGE,  message  != null ? message  : "");
        local.putExtra(EXTRA_LINE,     line);

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(local);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String translateEvent(String function, String status) {
        if (function == null) function = "";
        if (status   == null) status   = "";

        if (status.toLowerCase().contains("ringing"))     return EVT_RINGING;
        if (status.toLowerCase().contains("connected"))   return EVT_CONNECTED;
        if (status.toLowerCase().contains("disconnected") ||
            status.toLowerCase().contains("hangup") ||
            status.toLowerCase().contains("bye"))         return EVT_DISCONNECTED;

        switch (function.toLowerCase()) {
            case "api_register":
            case "api_registerex":
                if (status.toLowerCase().contains("ok") ||
                    status.toLowerCase().contains("registered")) return EVT_REGISTERED;
                if (status.toLowerCase().contains("fail") ||
                    status.toLowerCase().contains("error"))      return EVT_REG_FAILED;
                return EVT_STATUS;
            case "api_unregister":
                return EVT_UNREGISTERED;
            default:
                return EVT_STATUS;
        }
    }

    // ── Static registration helpers ───────────────────────────────────

    public static IntentFilter getFilter() {
        return new IntentFilter(SDK_ACTION);
    }

    public static IntentFilter getLocalFilter() {
        return new IntentFilter(LOCAL_ACTION);
    }

    /**
     * Register a local broadcast receiver for SDK events.
     * Call this in onResume(); unregister in onPause().
     */
    public static void registerLocal(Context ctx, BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(ctx)
                .registerReceiver(receiver, getLocalFilter());
    }

    public static void unregisterLocal(Context ctx, BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(ctx)
                .unregisterReceiver(receiver);
    }
}
