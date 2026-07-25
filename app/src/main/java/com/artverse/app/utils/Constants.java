package com.artverse.app.utils;

public final class Constants {
    private Constants() { }

    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_ARTIST = "artist";
    public static final String ROLE_ADMIN = "admin";

    /** Artist registration awaiting admin review (set at sign-up). */
    public static final String ARTIST_STATUS_PENDING = "pending";
    /** Admin approved the registration - full artist access granted. */
    public static final String ARTIST_STATUS_APPROVED = "approved";
    /** Admin rejected the registration. */
    public static final String ARTIST_STATUS_REJECTED = "rejected";

    /** Content review (artworks and staged edits) - same vocabulary. */
    public static final String REVIEW_STATUS_PENDING = "pending";
    public static final String REVIEW_STATUS_APPROVED = "approved";
    public static final String REVIEW_STATUS_REJECTED = "rejected";

    /** Legacy status - orders created before the processing/completed flow. Treated like processing. */
    public static final String STATUS_PENDING = "pending";
    /** Order placed, awaiting the artist's confirm/reject decision. Shown as "Pending". */
    public static final String STATUS_PROCESSING = "processing";
    /** Artist confirmed the order and is preparing it for hand-over to delivery. */
    public static final String STATUS_CONFIRMED = "confirmed";
    /** Artist handed the order to the delivery section; admin now tracks it. */
    public static final String STATUS_OUT_FOR_DELIVERY = "out_for_delivery";
    /** Admin confirmed delivery - sale settled and recorded in "transactions". */
    public static final String STATUS_COMPLETED = "completed";
    /** Rejected by the artist (before dispatch) or by the admin (in delivery). */
    public static final String STATUS_REJECTED = "rejected";

    public static final String EXTRA_ARTWORK_ID = "extra_artwork_id";
    /** Tells CustomerMainActivity to land on the Cart tab instead of Home. */
    public static final String EXTRA_OPEN_CART = "extra_open_cart";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";

    /* ---------- Full-screen artwork viewer (My Collection) ---------- */

    /** Title recorded on the order line - names the saved file. */
    public static final String EXTRA_ARTWORK_TITLE = "extra_artwork_title";
    /** Image from the order line, used until the live listing loads. */
    public static final String EXTRA_ARTWORK_IMAGE_URL = "extra_artwork_image_url";
    /** True for an owned digital piece, which the buyer may save to the device. */
    public static final String EXTRA_ALLOW_DOWNLOAD = "extra_allow_download";

    /* ---------- Push notification deep links ---------- */

    /** Tells the main activities to land on the Orders tab. */
    public static final String EXTRA_OPEN_ORDERS = "extra_open_orders";
    /** Screen a tapped notification should open (see ROUTE_*). */
    public static final String EXTRA_ROUTE = "extra_route";
    /** UID the notification was addressed to - verified before deep linking. */
    public static final String EXTRA_TARGET_UID = "extra_target_uid";

    /** Open the signed-in role's orders screen (artist incoming / customer history). */
    public static final String ROUTE_ORDERS = "orders";

    /** Payload keys used by the Cloud Function that sends the push. */
    public static final String PUSH_KEY_ROUTE = "route";
    public static final String PUSH_KEY_ORDER_ID = "orderId";
    public static final String PUSH_KEY_USER_ID = "userId";
    public static final String PUSH_KEY_NOTIFICATION_ID = "notificationId";
}
