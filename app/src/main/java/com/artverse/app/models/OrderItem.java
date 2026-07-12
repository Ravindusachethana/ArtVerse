package com.artverse.app.models;

public class OrderItem {
    public String artworkId;
    public String title;
    public String imageUrl;
    public double unitPrice;
    public int quantity;

    public OrderItem() { }

    public OrderItem(String artworkId, String title, String imageUrl, double unitPrice, int quantity) {
        this.artworkId = artworkId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
}
