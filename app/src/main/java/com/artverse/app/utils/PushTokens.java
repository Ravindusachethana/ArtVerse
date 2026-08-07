package com.artverse.app.utils;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Owns the device's FCM registration token.
 *
 * The token is stored on the recipient's own profile - {@code users/{uid}.fcmTokens}
 * - as an array, so one account can be signed in on several devices and every
 * one of them gets the push. The Cloud Function that fans out order
 * notifications reads exactly this field.
 *
 * The token is written on login and on every app start (cheap, idempotent
 * arrayUnion) and removed on sign-out, so a handset that someone signed out of
 * stops receiving that account's order alerts.
 */
public final class PushTokens {

    private static final String TAG = "PushTokens";
    private static final String FIELD = "fcmTokens";

    private PushTokens() { }

    /** Fetches this device's token and attaches it to the signed-in user. */
    public static void register() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> save(uid, token))
                .addOnFailureListener(e -> Log.w(TAG, "Could not obtain FCM token", e));
    }

    /** Attaches a known token to a user (also used from onNewToken). */
    public static void save(String uid, String token) {
        if (uid == null || token == null || token.isEmpty()) return;

        FirebaseUtil.usersRef().document(uid)
                .update(FIELD, FieldValue.arrayUnion(token))
                .addOnFailureListener(e -> Log.w(TAG, "Could not store FCM token", e));
    }

    /**
     * Detaches this device's token from the signed-in user. Resolves even on
     * failure so sign-out is never blocked by a network hiccup - a stale token
     * is pruned by the Cloud Function the first time a send to it fails.
     */
    public static Task<Void> unregister() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return Tasks.forResult(null);

        return FirebaseMessaging.getInstance().getToken()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
                    return FirebaseUtil.usersRef().document(uid)
                            .update(FIELD, FieldValue.arrayRemove(task.getResult()));
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) Log.w(TAG, "Could not clear FCM token", task.getException());
                    return null;
                });
    }
}
