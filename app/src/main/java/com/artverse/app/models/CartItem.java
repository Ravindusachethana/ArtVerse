package com.artverse.app.models;

public class CartItem {
    public String artworkId;
    public String title;
    public String imageUrl;
    public String artistId;
    public String artistName;
    public double price;
    public int quantity;

    public CartItem() { }

    public CartItem(String artworkId, String title, String imageUrl, String artistId, String artistName,
                     double price, int quantity) {
        this.artworkId = artworkId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.artistId = artistId;
        this.artistName = artistName;
        this.price = price;
        this.quantity = quantity;
    }

    public double lineTotal() {
        return price * quantity;
    }
}
