package com.artverse.app.services;

import androidx.annotation.NonNull;

import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.NotificationRouter;
import com.artverse.app.utils.PushTokens;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives the order pushes sent by the Cloud Function.
 *
 * Delivery differs by app state, and both paths are covered:
 *   - App closed or backgrounded: the message carries a notification block, so
 *     Android renders it from the system tray without waking the app at all.
 *     Tapping it launches SplashActivity with the data payload as extras
 *     (see NotificationRouter / SplashActivity for the routing + session check).
 *   - App in the foreground: no tray notification is drawn automatically, so
 *     onMessageReceived below posts it, wiring up the same deep link.
 */
public class ArtVerseMessagingService extends FirebaseMessagingService {

    /**
     * Fires when FCM rotates this device's token (reinstall, restore, cache
     * clear). Re-attach it so the account keeps receiving pushes.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushTokens.save(FirebaseUtil.currentUid(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();

        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : data.get("title");
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : data.get("message");
        if (title == null && body == null) return;

        // Only alert the account the notification was addressed to - a device
        // that has since been signed into another account must not show it.
        String targetUid = data.get(Constants.PUSH_KEY_USER_ID);
        String currentUid = FirebaseUtil.currentUid();
        if (targetUid != null && currentUid != null && !targetUid.equals(currentUid)) return;

        NotificationRouter.post(this,
                title != null ? title : "ArtVerse",
                body != null ? body : "",
                data.get(Constants.PUSH_KEY_NOTIFICATION_ID),
                NotificationRouter.deepLinkIntent(this, data));
    }
}
