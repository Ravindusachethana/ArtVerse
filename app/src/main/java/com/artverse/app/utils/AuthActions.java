package com.artverse.app.utils;

import android.content.Context;

/**
 * Shared sign-out sequence.
 *
 * Order matters: this device's push token has to be detached from the account
 * *while the user is still authenticated*, because the security rules only let
 * someone write their own profile document. Signing out first would leave the
 * token attached and the handset would keep receiving that account's order
 * alerts after they left.
 */
public final class AuthActions {

    private AuthActions() { }

    /**
     * Detaches the push token, clears the Firebase session and the local role
     * cache, then hands back to the caller to navigate. {@code onComplete}
     * always runs - a failed token cleanup must never strand the user in a
     * half-signed-out state (the Cloud Function prunes tokens that stop
     * accepting sends anyway).
     */
    public static void signOut(Context context, Runnable onComplete) {
        PushTokens.unregister().addOnCompleteListener(task -> {
            InAppNotifier.stop();
            FirebaseUtil.auth().signOut();
            new SessionManager(context).clear();
            onComplete.run();
        });
    }
}
