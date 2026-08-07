package com.artverse.app.models;

import java.util.List;

/**
 * Represents a customer order, scoped to a single artist (checkout splits the
 * cart per artist). Stored in "orders/{orderId}".
 *
 * Delivery tracking lifecycle:
 *   processing        - placed by the customer, awaiting the artist (shown as "Pending")
 *   confirmed         - artist accepted and is preparing it
 *   out_for_delivery  - artist handed it to the delivery section
 *   completed         - admin confirmed delivery; the sale is recorded here
 *   rejected          - declined by the artist, or by the admin during delivery
 *
 * The stage timestamps below drive the tracking bar shown on both the
 * customer's and the artist's order cards.
 */
public class Order {
    public String id;
    public String customerId;
    public String customerName;
    public String artistId;
    public String deliveryAddress;
    public List<OrderItem> items;
    public double totalAmount;
    public String status;       // processing, confirmed, out_for_delivery, completed, rejected
    public String paymentMethod;
    public long orderDate;

    /** Stage timestamps - 0 until the order reaches that stage. */
    public long confirmedAt;
    public long dispatchedAt;
    public long deliveredAt;
    /** UID of whoever settled the order (artist on reject, admin on delivery). */
    public String settledBy;

    public Order() { }

    public Order(String id, String customerId, String customerName, String artistId,
                 String deliveryAddress, List<OrderItem> items, double totalAmount,
                 String status, String paymentMethod, long orderDate) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.artistId = artistId;
        this.deliveryAddress = deliveryAddress;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.orderDate = orderDate;
    }
}
