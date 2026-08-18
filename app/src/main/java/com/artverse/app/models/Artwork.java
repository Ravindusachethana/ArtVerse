package com.artverse.app.models;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Map;

/**
 * single artwork listing.
 */
public class Artwork {
    public String id;
    public String title;
    public String description;
    public String categoryId;
    public String categoryName;
    public String artistId;
    public String artistName;
    public double price;
    public int quantity;
    public List<String> imageUrls;   // up to 5 images per Items entity
    public String medium;            // e.g. Oil on canvas
    public String dimensions;        // e.g. 60cm x 80cm
    public boolean available;
    public long createdAt;

    /**
     * Content moderation: "pending" | "approved" | "rejected"
     * (Constants.REVIEW_STATUS_*). New artworks start pending and are hidden
     * from buyers until the admin approves. Null on artworks listed before
     * this feature - treated as approved (legacy).
     */
    public String moderationStatus;

    /**
     * Staged edit of a published artwork awaiting admin review. The live
     * fields stay untouched; the admin panel merges this map into the
     * document on approval (or discards it on rejection).
     */
    public Map<String, Object> pendingChanges;

    public long reviewedAt;
    public String reviewedBy;

    /**
     * True once a one-of-a-kind piece (Painting, Drawing, Mixed Media) has been
     * sold and its delivery completed: there is only ever one, so it leaves the
     * shop. The document is kept - the buyer's collection and everyone's sales
     * reports still read it - it is only dropped from the browse gallery and the
     * artist's My Art listing. Set by the admin panel when it settles the
     * delivery (admin-web OrdersService.markDelivered); always false for
     * reproducible pieces and anything still for sale.
     */
    public boolean retired;

    public Artwork(String id, String title, String description, String category, String category1, String uid, String artistName, double price, double dPrice, int quantity, List<String> images, String medium, String dimensions, boolean b, long l) {
    }

    public Artwork(String id, String title, String description, String category, String category1, String uid, String artistName, double price, TextInputEditText dPrice, int quantity, List<String> images, String medium, String dimensions, boolean b, long l) {
    }

    /** True when buyers may see this listing. Static so Firestore ignores it. */
    public static boolean isPublished(Artwork artwork) {
        return artwork.moderationStatus == null
                || "approved".equals(artwork.moderationStatus);
    }

    /** True when an edit of this (published) artwork awaits admin review. */
    public static boolean hasPendingEdit(Artwork artwork) {
        return artwork.pendingChanges != null && !artwork.pendingChanges.isEmpty();
    }

    public Artwork() { }

    public Artwork(String id, String title, String description, String categoryId,
                    String categoryName, String artistId, String artistName, double price,
                    int quantity, List<String> imageUrls, String medium, String dimensions,
                    boolean available, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.artistId = artistId;
        this.artistName = artistName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrls = imageUrls;
        this.medium = medium;
        this.dimensions = dimensions;
        this.available = available;
        this.createdAt = createdAt;
    }
}
