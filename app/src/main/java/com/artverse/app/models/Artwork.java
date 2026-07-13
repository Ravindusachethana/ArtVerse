package com.artverse.app.models;

import java.util.List;

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
