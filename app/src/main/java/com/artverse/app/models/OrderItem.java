package com.artverse.app.models;

public class OrderItem {
    public String artworkId;
    public String title;
    public String imageUrl;
    /**
     * Category as it was when the piece was bought. Stored on the order line so
     * the buyer's collection can tell a digital piece from a physical one
     * without re-reading the artwork - and still knows it if the listing is
     * later edited or removed. Null on orders placed before this field existed.
     */
    public String categoryName;
    public double unitPrice;
    public int quantity;

    public OrderItem() { }

    public OrderItem(String artworkId, String title, String imageUrl, String categoryName,
                     double unitPrice, int quantity) {
        this.artworkId = artworkId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
}
