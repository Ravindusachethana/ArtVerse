package com.artverse.app.utils;

import com.artverse.app.models.AppNotification;
import com.artverse.app.models.Order;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Single home for the order lifecycle mutations, shared by the artist
 * dashboard and Incoming Orders screens so the delivery flow behaves
 * identically everywhere (part of FR07/FR09).
 *
 * Lifecycle: checkout creates the order as "processing" (stock already
 * reserved) and notifies the artist. The artist then confirms it, and hands
 * it over to the delivery section. From there the admin tracks it and marks
 * it delivered - that is where the sale is settled (Transaction records and
 * the artist's totalSales credit), so the Sales Report only ever counts
 * artwork that actually reached the buyer. Cancelling an order is the admin's
 * job (the web panel releases the reserved stock on reject); the artist has no
 * reject action. Every stage change notifies the customer.
 */
public final class OrderActions {

    private OrderActions() { }

    /**
     * Artist confirms a new order. This does not settle the sale - it only
     * moves the order to "confirmed" so the artist can prepare it.
     */
    public static Task<Void> confirm(Order order) {
        long now = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Constants.STATUS_CONFIRMED);
        updates.put("confirmedAt", now);

        return FirebaseUtil.ordersRef().document(order.id).update(updates)
                .addOnSuccessListener(v -> notifyCustomer(order, "Order confirmed",
                        "The artist confirmed order #" + shortId(order.id)
                                + " and is preparing it for delivery.", now));
    }

    /**
     * Artist hands the confirmed order to the delivery section. From this
     * point the admin owns the order's remaining tracking.
     */
    public static Task<Void> handOverToDelivery(Order order) {
        long now = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Constants.STATUS_OUT_FOR_DELIVERY);
        updates.put("dispatchedAt", now);
        if (order.confirmedAt == 0) updates.put("confirmedAt", now);

        return FirebaseUtil.ordersRef().document(order.id).update(updates)
                .addOnSuccessListener(v -> notifyCustomer(order, "Out for delivery",
                        "Order #" + shortId(order.id) + " is on its way to you.", now));
    }

    /**
     * Fire-and-forget message to the buyer, sent *after* the stage change has
     * committed rather than inside it. Batched writes are all-or-nothing, so
     * bundling the notification meant a blocked notification write would fail
     * the whole confirm/dispatch. The buyer's order screen listens to
     * Firestore anyway, so it updates even if this message never lands.
     */
    private static void notifyCustomer(Order order, String title, String message, long when) {
        if (order.customerId == null) return;
        DocumentReference ref = FirebaseUtil.notificationsRef().document();
        ref.set(new AppNotification(ref.getId(), order.customerId, order.id,
                title, message, false, when));
    }

    /** Queue a notification inside an existing batch. */
    public static void writeNotification(WriteBatch batch, String recipientId, String orderId,
                                         String title, String message, long when) {
        DocumentReference ref = FirebaseUtil.notificationsRef().document();
        batch.set(ref, new AppNotification(ref.getId(), recipientId, orderId, title, message, false, when));
    }

    /** Queue a notification inside an existing Firestore transaction (used by checkout). */
    public static void writeNotification(com.google.firebase.firestore.Transaction firestoreTransaction,
                                         String recipientId, String orderId,
                                         String title, String message, long when) {
        DocumentReference ref = FirebaseUtil.notificationsRef().document();
        firestoreTransaction.set(ref, new AppNotification(ref.getId(), recipientId, orderId, title, message, false, when));
    }

    public static String invoiceNumber(long when, String artworkId) {
        return "INV-" + when + "-" + artworkId.substring(0, Math.min(5, artworkId.length()));
    }

    public static String shortId(String orderId) {
        return orderId != null && orderId.length() >= 6
                ? orderId.substring(0, 6).toUpperCase(Locale.ROOT) : String.valueOf(orderId);
    }
}
