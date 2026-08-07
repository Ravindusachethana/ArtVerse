package com.artverse.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the artwork-detail quantity stepper arithmetic (Chapter 5,
 * Unit Testing - module: QuantitySelector). Covers the "live quantity and
 * price update, capped to remaining stock" requirement.
 */
public class QuantitySelectorTest {

    @Test
    public void nextQuantity_incrementsWithinStock() {
        assertEquals(2, QuantitySelector.nextQuantity(1, 1, 5));
    }

    @Test
    public void nextQuantity_decrementsAboveFloor() {
        assertEquals(2, QuantitySelector.nextQuantity(3, -1, 5));
    }

    @Test
    public void nextQuantity_refusesToGoBelowOne() {
        assertEquals(1, QuantitySelector.nextQuantity(1, -1, 5));
    }

    @Test
    public void nextQuantity_refusesToExceedRemainingStock() {
        assertEquals(5, QuantitySelector.nextQuantity(5, 1, 5));
    }

    @Test
    public void nextQuantity_singlePieceStockNeverIncreases() {
        assertEquals(1, QuantitySelector.nextQuantity(1, 1, 1));
    }

    @Test
    public void canDecrease_falseAtFloor() {
        assertFalse(QuantitySelector.canDecrease(1));
        assertTrue(QuantitySelector.canDecrease(2));
    }

    @Test
    public void canIncrease_falseWhenAtRemainingStock() {
        assertFalse(QuantitySelector.canIncrease(5, 5));
        assertTrue(QuantitySelector.canIncrease(4, 5));
    }

    @Test
    public void lineTotal_multipliesUnitPriceByQuantity() {
        assertEquals(72000.0, QuantitySelector.lineTotal(24000.0, 3), 0.001);
    }

    @Test
    public void lineTotal_zeroQuantityIsZero() {
        assertEquals(0.0, QuantitySelector.lineTotal(24000.0, 0), 0.001);
    }
}
