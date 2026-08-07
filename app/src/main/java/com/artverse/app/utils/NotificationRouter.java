package com.artverse.app.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.artverse.app.R;
import com.artverse.app.auth.SplashActivity;

import java.util.Map;

/**
 * Single place that knows how an order alert is presented and where tapping it
 * leads, shared by the push path (ArtVerseMessagingService) and the in-app
 * fallback (InAppNotifier).
 *
 * Every tap re-enters through {@link SplashActivity}, which already owns the
 * session check and the artist-approval gate. That keeps one routing authority:
 * the notification only says *where* the user wanted to go, and Splash decides
 * whether they may go there (signed in? same account? approved artist?).
 */
public final class NotificationRouter {

    public static final String CHANNEL_ID = "artverse_orders";

    private NotificationRouter() { }

    /**
     * Intent for a tapped notification. It always targets the launcher
     * activity so the app comes up with a valid task stack whether it was
     * backgrounded or fully closed.
     */
    public static Intent deepLinkIntent(Context context, String route, String orderId, String targetUid) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.EXTRA_ROUTE, route != null ? route : Constants.ROUTE_ORDERS);
        if (orderId != null) intent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
        if (targetUid != null) intent.putExtra(Constants.EXTRA_TARGET_UID, targetUid);
        return intent;
    }

    /** Builds the deep link straight from an FCM data payload. */
    public static Intent deepLinkIntent(Context context, Map<String, String> data) {
        return deepLinkIntent(context,
                data.get(Constants.PUSH_KEY_ROUTE),
                data.get(Constants.PUSH_KEY_ORDER_ID),
                data.get(Constants.PUSH_KEY_USER_ID));
    }

    /**
     * Posts an order alert.
     *
     * @param dedupeKey the notification document id - reused as the system
     *                  notification id so the push copy and the in-app
     *                  fallback copy replace each other instead of stacking.
     * @return true when it was actually shown (false when the user has not
     *         granted POST_NOTIFICATIONS yet, so callers can retry later).
     */
    public static boolean post(Context context, String title, String message,
                               String dedupeKey, Intent deepLink) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        createChannel(context);

        int id = dedupeKey != null ? dedupeKey.hashCode() : (int) System.currentTimeMillis();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, deepLink, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orders)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(id, builder.build());
        return true;
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Order updates",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("New orders and order accept/reject alerts");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
