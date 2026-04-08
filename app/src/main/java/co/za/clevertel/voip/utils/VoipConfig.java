package co.za.clevertel.voip.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.za.clevertel.voip.BuildConfig;

/**
 * VoipConfig
 *
 * Central place for all MizuVoIP JVoIP SDK configuration.
 * Uses BroadcastAPI (Intent-based) to send commands to the SDK.
 *
 * Key settings applied:
 *  - serveraddress  → sip.clevertel.co.za (hardcoded via BuildConfig)
 *  - forcewifi      → auto  (prefer WiFi, fall back to mobile data)
 *  - background     → yes   (keep SIP registered when app is backgrounded)
 *  - foreground     → yes   (run as foreground service — "foreign" mode)
 *  - pushtype       → fcm_gateway (push via MizuVoIP FCM gateway)
 *  - autostart      → 2    (restart after kill / boot)
 */
public class VoipConfig {

    private static final String TAG = "VoipConfig";
    private static final String SIP_DOMAIN = BuildConfig.SIP_DOMAIN; // sip.clevertel.co.za
    private static final String BROADCAST_ACTION = "co.za.clevertel.voip.JVOIP_EVENT";

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Apply all global SDK settings. Call once at Application.onCreate()
     * before any SIP registration attempt.
     */
    public static void applyGlobalSettings(Context ctx) {
        // 1. Static SIP server address
        sendParam(ctx, "serveraddress", SIP_DOMAIN);

        // 2. Force WiFi = auto (use WiFi when available, mobile data as fallback)
        sendParam(ctx, "forcewifi", "auto");

        // 3. Background / foreground service mode
        sendParam(ctx, "background", "yes");      // stay registered in background
        sendParam(ctx, "foreground", "yes");       // run as foreground service (foreign)
        sendParam(ctx, "backgroundcalls", "yes");  // accept calls while in background

        // 4. Push notifications via FCM gateway
        sendParam(ctx, "pushtype",            "fcm_gateway");
        sendParam(ctx, "pushnotifications",   "yes");
        sendParam(ctx, "fcmgateway",          "fcm.webvoipphone.com");

        // 5. Auto-restart: 2 = restart on task kill AND on boot
        sendParam(ctx, "autostart",           "2");
        sendParam(ctx, "startonboot",         "true");

        // 6. Keep-alive so NAT mappings stay open
        sendParam(ctx, "keepalive",           "yes");
        sendParam(ctx, "natkeepalive",        "yes");
        sendParam(ctx, "keepaliveival",       "30");

        // 7. App identity for the SDK
        sendParam(ctx, "appname", "Clevertel VoIP");
        sendParam(ctx, "appid",   "co.za.clevertel.voip");

        Log.i(TAG, "Global VoIP settings applied. SIP domain: " + SIP_DOMAIN);
    }

    /**
     * Register / re-register a SIP account.
     * Domain is always sip.clevertel.co.za — not exposed to the user.
     */
    public static void registerAccount(Context ctx, String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            Log.w(TAG, "registerAccount: empty credentials, skipping");
            return;
        }

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function",    "API_SetCredentials");
        intent.putExtra("server",      SIP_DOMAIN);
        intent.putExtra("username",    username.trim());
        intent.putExtra("password",    password);
        intent.putExtra("authname",    username.trim());
        intent.putExtra("displayname", "Clevertel VoIP");
        ctx.sendBroadcast(intent);

        // Trigger registration
        Intent reg = new Intent(BROADCAST_ACTION);
        reg.putExtra("function", "API_Register");
        ctx.sendBroadcast(reg);

        Log.i(TAG, "SIP registration requested for: " + username + "@" + SIP_DOMAIN);
    }

    /**
     * Unregister the SIP account (logout).
     */
    public static void unregisterAccount(Context ctx) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Unregister");
        ctx.sendBroadcast(intent);
        Log.i(TAG, "SIP unregistered");
    }

    /**
     * Stop the SIP stack entirely.
     */
    public static void stopStack(Context ctx) {
        unregisterAccount(ctx);
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Stop");
        ctx.sendBroadcast(intent);
        Log.i(TAG, "SIP stack stopped");
    }

    /**
     * Register the FCM token with the push gateway so incoming calls
     * wake the app even when not in the foreground.
     */
    public static void registerPushToken(Context ctx, String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) return;

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function",         "API_SetPushNotifications");
        intent.putExtra("pushnotifications", fcmToken);
        ctx.sendBroadcast(intent);

        Log.i(TAG, "FCM push token registered with gateway");
    }

    /**
     * Initiate an outbound call.
     */
    public static void call(Context ctx, String number) {
        if (number == null || number.trim().isEmpty()) return;

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Call");
        intent.putExtra("callto",   number.trim());
        ctx.sendBroadcast(intent);
        Log.i(TAG, "Outbound call to: " + number);
    }

    /**
     * Hang up the active call.
     */
    public static void hangup(Context ctx) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Hangup");
        ctx.sendBroadcast(intent);
    }

    /**
     * Accept an incoming call.
     */
    public static void accept(Context ctx) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Accept");
        ctx.sendBroadcast(intent);
    }

    /**
     * Reject an incoming call.
     */
    public static void reject(Context ctx) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Reject");
        ctx.sendBroadcast(intent);
    }

    /**
     * Toggle mute on the active call.
     */
    public static void toggleMute(Context ctx, boolean mute) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Mute");
        intent.putExtra("param",    mute ? "1" : "0");
        ctx.sendBroadcast(intent);
    }

    /**
     * Toggle hold on the active call.
     */
    public static void toggleHold(Context ctx, boolean hold) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_Hold");
        intent.putExtra("param",    hold ? "1" : "0");
        ctx.sendBroadcast(intent);
    }

    /**
     * Send a DTMF digit during a call.
     */
    public static void sendDtmf(Context ctx, String digit) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_DTMF");
        intent.putExtra("param",    digit);
        ctx.sendBroadcast(intent);
    }

    /**
     * Query the current registration/line status.
     * Listen for the SDK's broadcast reply for STATUS events.
     */
    public static void requestStatus(Context ctx) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_GetStatus");
        ctx.sendBroadcast(intent);
    }

    // ── Internal helper ───────────────────────────────────────────────

    private static void sendParam(Context ctx, String key, String value) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("function", "API_SetParameter");
        intent.putExtra("param",    key);
        intent.putExtra("value",    value);
        ctx.sendBroadcast(intent);
    }
}
