package com.artverse.app.utils;

import android.content.Context;

import com.artverse.app.models.AppNotification;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Fallback alerting for entries the push path did not deliver.
 *
 * Order alerts normally arrive as an FCM push sent by the Cloud Function
 * watching "notifications" (that is what reaches a closed app). The function
 * flips {@code delivered} to true once a push has been accepted for at least
 * one device, so this listener - which only ever queries undelivered entries -
 * stays quiet for them and no alert is shown twice.
 *
 * It still matters when the push could not be sent: the recipient had no
 * registered device (never opened the app since install), the token was stale,
 * or the Cloud Function is not deployed. Those entries remain undelivered and
 * are surfaced here the next time the user opens the app. Notifications are
 * posted under the notification document's id, so a push copy and a fallback
 * copy of the same alert replace each other rather than stacking.
 */
public final class InAppNotifier {

    private static ListenerRegistration registration;

    private InAppNotifier() { }

    public static void start(Context context) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        NotificationRouter.createChannel(context);
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
     * Returns true when the notification was actually shown. When the user has
     * not granted POST_NOTIFICATIONS yet, the entry stays undelivered so it is
     * retried once permission is granted.
     */
    private static boolean post(Context context, AppNotification notification) {
        return NotificationRouter.post(context,
                notification.title,
                notification.message,
                notification.id,
                NotificationRouter.deepLinkIntent(context, Constants.ROUTE_ORDERS,
                        notification.orderId, notification.userId));
    }
}
