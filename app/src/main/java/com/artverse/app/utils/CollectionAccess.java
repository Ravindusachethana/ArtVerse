package com.artverse.app.utils;

/**
 * Decides when a bought piece belongs in the Art Lover's collection, and
 * whether they may download the file.
 *
 * Digital pieces (Digital Art, Photography) have nothing to ship - the file is
 * the artwork - so they are handed over inside the app as soon as the artist
 * confirms the order, without waiting for the delivery stages a physical piece
 * has to travel through. A physical piece joins the collection once the admin
 * confirms it was actually delivered.
 *
 * Kept free of Android and Firebase dependencies so the rule can be covered by
 * plain JVM unit tests (see CollectionAccessTest).
 */
public final class CollectionAccess {

    private CollectionAccess() { }

    /** True once the buyer owns this line of the order. */
    public static boolean isOwned(String orderStatus, String categoryName) {
        if (Constants.STATUS_COMPLETED.equals(orderStatus)) return true;
        if (!ArtCategories.isDigital(categoryName)) return false;

        // Digital: the artist confirming the order is the delivery.
        return Constants.STATUS_CONFIRMED.equals(orderStatus)
                || Constants.STATUS_OUT_FOR_DELIVERY.equals(orderStatus);
    }

    /** True when the buyer can save the original file to their device. */
    public static boolean isDownloadable(String orderStatus, String categoryName) {
        return ArtCategories.isDigital(categoryName) && isOwned(orderStatus, categoryName);
    }

    /**
     * How the piece reached the buyer, shown under the collection tile so a
     * digital piece that arrived before its order finished tracking does not
     * look like a mistake.
     */
    public static String deliveryLabel(String categoryName) {
        return ArtCategories.isDigital(categoryName) ? "Delivered in app" : "Delivered";
    }
}
