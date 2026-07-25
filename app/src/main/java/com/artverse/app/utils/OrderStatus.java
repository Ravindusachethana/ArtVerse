package com.artverse.app.utils;

import com.artverse.app.R;

/**
 * Single source of truth for how an order's delivery stage is presented, so
 * the customer's and the artist's order cards can never drift apart.
 *
 * Tracking stages: Placed -> Confirmed -> Out for Delivery -> Delivered.
 * A rejected order leaves the track entirely (step {@link #STEP_REJECTED}).
 */
public final class OrderStatus {

    /** Tracking bar positions. */
    public static final int STEP_PLACED = 0;
    public static final int STEP_CONFIRMED = 1;
    public static final int STEP_OUT_FOR_DELIVERY = 2;
    public static final int STEP_DELIVERED = 3;
    public static final int STEP_REJECTED = -1;

    /** Labels under the tracking bar. */
    public static final String[] STEP_LABELS = {"Placed", "Confirmed", "Out for delivery", "Delivered"};

    private OrderStatus() { }

    private static String safe(String status) {
        return status != null ? status : "";
    }

    public static int trackerStep(String status) {
        switch (safe(status)) {
            case Constants.STATUS_CONFIRMED: return STEP_CONFIRMED;
            case Constants.STATUS_OUT_FOR_DELIVERY: return STEP_OUT_FOR_DELIVERY;
            case Constants.STATUS_COMPLETED: return STEP_DELIVERED;
            case Constants.STATUS_REJECTED: return STEP_REJECTED;
            default: return STEP_PLACED;   // processing + legacy pending
        }
    }

    /** Pill text the buyer sees. */
    public static String customerLabel(String status) {
        switch (safe(status)) {
            case Constants.STATUS_CONFIRMED: return "Confirmed";
            case Constants.STATUS_OUT_FOR_DELIVERY: return "Out for delivery";
            case Constants.STATUS_COMPLETED: return "Delivered";
            case Constants.STATUS_REJECTED: return "Rejected";
            default: return "Pending";
        }
    }

    /** Pill text the artist sees - an unconfirmed order is "New" to them. */
    public static String artistLabel(String status) {
        return Constants.STATUS_PROCESSING.equals(safe(status))
                || Constants.STATUS_PENDING.equals(safe(status))
                ? "New" : customerLabel(status);
    }

    public static int pillBackground(String status) {
        switch (safe(status)) {
            case Constants.STATUS_CONFIRMED: return R.drawable.bg_pill_processing;
            case Constants.STATUS_OUT_FOR_DELIVERY: return R.drawable.bg_pill_delivery;
            case Constants.STATUS_COMPLETED: return R.drawable.bg_pill_completed;
            case Constants.STATUS_REJECTED: return R.drawable.bg_pill_rejected;
            default: return R.drawable.bg_pill_pending;
        }
    }

    public static int pillColor(String status) {
        switch (safe(status)) {
            case Constants.STATUS_CONFIRMED: return R.color.status_processing;
            case Constants.STATUS_OUT_FOR_DELIVERY: return R.color.brand_primary;
            case Constants.STATUS_COMPLETED: return R.color.status_success;
            case Constants.STATUS_REJECTED: return R.color.status_rejected;
            default: return R.color.status_pending;
        }
    }

    /** Awaiting the artist's confirm/reject decision. */
    public static boolean isAwaitingArtist(String status) {
        return Constants.STATUS_PROCESSING.equals(safe(status))
                || Constants.STATUS_PENDING.equals(safe(status));
    }

    /** Confirmed but not yet handed to the delivery section. */
    public static boolean isConfirmed(String status) {
        return Constants.STATUS_CONFIRMED.equals(safe(status));
    }

    public static boolean isOutForDelivery(String status) {
        return Constants.STATUS_OUT_FOR_DELIVERY.equals(safe(status));
    }

    /** Finished either way - these open a receipt. */
    public static boolean isSettled(String status) {
        return Constants.STATUS_COMPLETED.equals(safe(status))
                || Constants.STATUS_REJECTED.equals(safe(status));
    }
}
