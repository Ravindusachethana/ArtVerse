package com.artverse.app.utils;

/**
 * Pure quantity/stock arithmetic shared by the artwork detail quantity
 * stepper. Kept free of any Android dependency so it can be covered by plain
 * JVM unit tests (see QuantitySelectorTest).
 */
public final class QuantitySelector {

    private QuantitySelector() { }

    /** Applies delta (+1/-1) but refuses to leave the [1, maxStock] range - a no-op at either edge. */
    public static int nextQuantity(int current, int delta, int maxStock) {
        int candidate = current + delta;
        if (candidate < 1 || candidate > maxStock) return current;
        return candidate;
    }

    public static boolean canDecrease(int current) {
        return current > 1;
    }

    public static boolean canIncrease(int current, int maxStock) {
        return current < maxStock;
    }

    public static double lineTotal(double unitPrice, int quantity) {
        return unitPrice * quantity;
    }
}
