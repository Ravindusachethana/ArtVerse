package com.artverse.app.models;

/**
 * Settled sale record for the audit trail described in the dissertation
 * (Section 2.1.3.2) and feeds the artist Sales Tracking module (FR09).
 */
public class Transaction {
    public String id;
    public String orderId;
    public String artworkId;
    public String artistId;
    public String invoiceNumber;
    public int quantity;
    public double unitPrice;
    public double total;
    public long date;

    public Transaction() { }

    public Transaction(String id, String orderId, String artworkId, String artistId,
                        String invoiceNumber, int quantity, double unitPrice, double total, long date) {
        this.id = id;
        this.orderId = orderId;
        this.artworkId = artworkId;
        this.artistId = artistId;
        this.invoiceNumber = invoiceNumber;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.date = date;
    }
}
