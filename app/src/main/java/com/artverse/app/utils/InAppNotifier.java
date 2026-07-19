package com.artverse.app.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.artverse.app.R;
import com.artverse.app.models.AppNotification;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Watches "notifications" for the signed-in user and surfaces each
 * undelivered entry as a system notification, then marks it delivered.
 * Started/stopped by the two main activities, so both roles get alerted
 * while the app is open; entries created while the user is offline are
 * delivered on their next launch (the initial snapshot replays them).
 */
public final class InAppNotifier {

    private static final String CHANNEL_ID = "artverse_orders";
    private static ListenerRegistration registration;

    private InAppNotifier() { }

    public static void start(Context context) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        createChannel(context);
        stop();

        Context appContext = context.getApplicationContext();
        registration = FirebaseUtil.notificationsRef()
                .whereEqualTo("userId", uid)
                .whereEqualTo("delivered", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;
                        AppNotification notification = change.getDocument().toObject(AppNotification.class);
                        if (post(appContext, notification)) {
                            change.getDocument().getReference().update("delivered", true);
                        }
                    }
                });
    }

    public static void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /**
     * Returns true when the notification was actually shown. When the user
     * has not granted POST_NOTIFICATIONS yet, the entry stays undelivered so
     * it is retried once permission is granted.
     */
    private static boolean post(Context context, AppNotification notification) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orders)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.message))
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(
                notification.id != null ? notification.id.hashCode() : (int) notification.createdAt,
                builder.build());
        return true;
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Order updates",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("New orders and order accept/reject alerts");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
