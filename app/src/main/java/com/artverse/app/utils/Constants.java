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
    /** Order placed, awaiting the artist's accept/reject decision. */
    public static final String STATUS_PROCESSING = "processing";
    /** Artist accepted - sale settled and recorded in "transactions". */
    public static final String STATUS_COMPLETED = "completed";
    /** Artist rejected - reserved stock released back to the artwork. */
    public static final String STATUS_REJECTED = "rejected";

    public static final String EXTRA_ARTWORK_ID = "extra_artwork_id";
    /** Tells CustomerMainActivity to land on the Cart tab instead of Home. */
    public static final String EXTRA_OPEN_CART = "extra_open_cart";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";
}
