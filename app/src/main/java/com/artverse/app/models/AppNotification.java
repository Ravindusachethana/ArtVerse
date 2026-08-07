package com.artverse.app.models;

/**
 * In-app notification stored in "notifications/{id}". Written in the same
 * Firestore batch/transaction as the order change that caused it, so a
 * notification never exists for a half-applied change. "delivered" flips to
 * true once the recipient's device has shown it (see InAppNotifier).
 */
public class AppNotification {
    public String id;
    public String userId;     // recipient uid (artist or customer)
    public String orderId;
    public String title;
    public String message;
    public boolean delivered;
    public long createdAt;

    public AppNotification() { }

    public AppNotification(String id, String userId, String orderId, String title,
                           String message, boolean delivered, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.orderId = orderId;
        this.title = title;
        this.message = message;
        this.delivered = delivered;
        this.createdAt = createdAt;
    }
}
