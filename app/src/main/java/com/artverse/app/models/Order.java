package com.artverse.app.models;

import java.util.List;

/**
 * Represents a customer order that may span items from one or more artists.
 * Stored in "orders/{orderId}". Status flows: pending -> processing -> completed / rejected.
 */
public class Order {
    public String id;
    public String customerId;
    public String customerName;
    public String artistId;
    public String deliveryAddress;
    public List<OrderItem> items;
    public double totalAmount;
    public String status;       // pending, processing, completed, rejected
    public String paymentMethod;
    public long orderDate;

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
