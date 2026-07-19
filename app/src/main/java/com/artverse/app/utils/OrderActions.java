package com.artverse.app.utils;

import com.artverse.app.models.AppNotification;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.models.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Single home for the order lifecycle mutations, shared by the artist
 * dashboard and Incoming Orders screens so accepting or rejecting an order
 * behaves identically everywhere (part of FR07/FR09).
 *
 * Lifecycle: checkout creates the order as "processing" (stock already
 * reserved) and notifies the artist. accept() settles it as "completed" -
 * only then are the Transaction audit records written and the artist's
 * totalSales credited, so the Sales Report never counts unaccepted orders.
 * reject() releases the reserved stock. Both decisions notify the customer.
 */
public final class OrderActions {

    private OrderActions() { }

    /** Artist accepts: order completes, sale is recorded, customer notified. All-or-nothing batch. */
    public static Task<Void> accept(Order order) {
        WriteBatch batch = FirebaseUtil.db().batch();
        long now = System.currentTimeMillis();

        batch.update(FirebaseUtil.ordersRef().document(order.id), "status", Constants.STATUS_COMPLETED);

        if (order.items != null) {
            for (OrderItem item : order.items) {
                DocumentReference txRef = FirebaseUtil.transactionsRef().document();
                Transaction tx = new Transaction(txRef.getId(), order.id, item.artworkId,
                        order.artistId, invoiceNumber(now, item.artworkId), item.quantity,
                        item.unitPrice, item.unitPrice * item.quantity, now);
                batch.set(txRef, tx);
            }
        }

        batch.update(FirebaseUtil.artistsRef().document(order.artistId),
                "totalSales", FieldValue.increment(order.totalAmount));

        writeNotification(batch, order.customerId, order.id, "Order completed",
                "Great news! Order #" + shortId(order.id) + " was accepted by the artist.", now);

        return batch.commit();
    }

    /** Artist rejects: order is closed, reserved stock is released, customer notified. */
    public static Task<Void> reject(Order order) {
        WriteBatch batch = FirebaseUtil.db().batch();
        long now = System.currentTimeMillis();

        batch.update(FirebaseUtil.ordersRef().document(order.id), "status", Constants.STATUS_REJECTED);

        if (order.items != null) {
            for (OrderItem item : order.items) {
                Map<String, Object> restore = new HashMap<>();
                restore.put("quantity", FieldValue.increment(item.quantity));
                restore.put("available", true);
                batch.update(FirebaseUtil.artworksRef().document(item.artworkId), restore);
            }
        }

        writeNotification(batch, order.customerId, order.id, "Order rejected",
                "Sorry, order #" + shortId(order.id) + " was declined by the artist. "
                        + "You have not been charged.", now);

        return batch.commit();
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
