package com.artverse.app.models;

/**
 * One artwork the Art Lover owns, shown in My Collection. Built in memory from
 * an order line - never stored in Firestore.
 *
 * The order line is the authority on what was bought (title, price, and the
 * category as it stood at purchase), so the entry survives the artist editing
 * or removing the listing afterwards.
 */
public class CollectionEntry {

    public String artworkId;
    public String title;
    public String categoryName;

    /** Image recorded on the order line - the artist's uploaded file. */
    public String imageUrl;

    public String orderId;
    public String orderStatus;
    public long purchasedAt;
    public int quantity;
    public double unitPrice;

    /** Digital piece the buyer may save to their device. */
    public boolean downloadable;
}
