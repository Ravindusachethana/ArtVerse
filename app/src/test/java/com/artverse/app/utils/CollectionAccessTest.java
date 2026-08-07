package com.artverse.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the rule that decides what appears in the Art Lover's
 * collection and what they may download (Chapter 5, Unit Testing - module:
 * CollectionAccess).
 */
public class CollectionAccessTest {

    private static final String DIGITAL = "Digital Art";
    private static final String PHOTO = "Photography";
    private static final String PHYSICAL = "Painting";

    @Test
    public void physicalPiece_joinsCollectionOnlyOnceDelivered() {
        assertTrue(CollectionAccess.isOwned(Constants.STATUS_COMPLETED, PHYSICAL));

        assertFalse(CollectionAccess.isOwned(Constants.STATUS_PROCESSING, PHYSICAL));
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_CONFIRMED, PHYSICAL));
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_OUT_FOR_DELIVERY, PHYSICAL));
    }

    @Test
    public void digitalPiece_joinsCollectionAsSoonAsTheArtistConfirms() {
        // Nothing to ship - confirming the order is the delivery.
        assertTrue(CollectionAccess.isOwned(Constants.STATUS_CONFIRMED, DIGITAL));
        assertTrue(CollectionAccess.isOwned(Constants.STATUS_OUT_FOR_DELIVERY, DIGITAL));
        assertTrue(CollectionAccess.isOwned(Constants.STATUS_COMPLETED, PHOTO));
    }

    @Test
    public void digitalPiece_isNotOwnedWhileTheArtistHasNotDecided() {
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_PROCESSING, DIGITAL));
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_PENDING, PHOTO));
    }

    @Test
    public void rejectedOrder_neverEntersTheCollection() {
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_REJECTED, DIGITAL));
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_REJECTED, PHOTO));
        assertFalse(CollectionAccess.isOwned(Constants.STATUS_REJECTED, PHYSICAL));
    }

    @Test
    public void unknownStatus_isNotOwned() {
        assertFalse(CollectionAccess.isOwned(null, DIGITAL));
        assertFalse(CollectionAccess.isOwned("", PHYSICAL));
    }

    @Test
    public void onlyOwnedDigitalPieces_canBeDownloaded() {
        assertTrue(CollectionAccess.isDownloadable(Constants.STATUS_CONFIRMED, DIGITAL));
        assertTrue(CollectionAccess.isDownloadable(Constants.STATUS_COMPLETED, PHOTO));

        // Delivered, but there is no file to hand over.
        assertFalse(CollectionAccess.isDownloadable(Constants.STATUS_COMPLETED, PHYSICAL));
        // Digital, but not paid for yet.
        assertFalse(CollectionAccess.isDownloadable(Constants.STATUS_PROCESSING, DIGITAL));
        assertFalse(CollectionAccess.isDownloadable(Constants.STATUS_REJECTED, DIGITAL));
    }

    @Test
    public void deliveryLabel_distinguishesInAppDelivery() {
        assertEquals("Delivered in app", CollectionAccess.deliveryLabel(DIGITAL));
        assertEquals("Delivered", CollectionAccess.deliveryLabel(PHYSICAL));
    }
}
