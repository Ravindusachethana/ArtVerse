package com.artverse.app.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Unit tests for CartItem (Chapter 5, Unit Testing - module: CartItem). */
public class CartItemTest {

    @Test
    public void lineTotal_multipliesPriceByQuantity() {
        CartItem item = new CartItem("art1", "Sunset Over Kandy", null, "Painting",
                "artist1", "R. Yatawaththa", 12500.0, 3);

        assertEquals(37500.0, item.lineTotal(), 0.001);
    }

    @Test
    public void lineTotal_reflectsQuantityUpdatedAfterConstruction() {
        CartItem item = new CartItem("art1", "Sunset Over Kandy", null, "Painting",
                "artist1", "R. Yatawaththa", 12500.0, 1);

        item.quantity = 4; // as CartFragment's quantity stepper does
        assertEquals(50000.0, item.lineTotal(), 0.001);
    }

    @Test
    public void lineTotal_zeroPriceIsZero() {
        CartItem item = new CartItem("art1", "Free Sketch", null, "Drawing",
                "artist1", "R. Yatawaththa", 0.0, 5);

        assertEquals(0.0, item.lineTotal(), 0.001);
    }

    @Test
    public void lineTotal_digitalLineIsAlwaysASingleCopy() {
        // Digital pieces have no stepper, so the line stays at one copy.
        CartItem item = new CartItem("art1", "Neon Colombo", null, "Digital Art",
                "artist1", "R. Yatawaththa", 8000.0, 1);

        assertEquals(8000.0, item.lineTotal(), 0.001);
    }
}
