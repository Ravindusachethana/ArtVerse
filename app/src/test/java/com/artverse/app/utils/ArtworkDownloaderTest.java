package com.artverse.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the naming a downloaded artwork is saved under (Chapter 5,
 * Unit Testing - module: ArtworkDownloader). The MediaStore write itself needs
 * a device, so only the pure naming rules are covered here.
 */
public class ArtworkDownloaderTest {

    /** A Firebase Storage download URL, as stored on the artwork. */
    private static final String STORAGE_URL =
            "https://firebasestorage.googleapis.com/v0/b/artverse.appspot.com/o/"
                    + "artwork_images%2Fabc123%2Fcover.jpg?alt=media&token=6d1f-91c2";

    @Test
    public void extensionOf_readsPastTheQueryString() {
        // The "?alt=media&token=..." tail must not be mistaken for the name.
        assertEquals(".jpg", ArtworkDownloader.extensionOf(STORAGE_URL));
    }

    @Test
    public void extensionOf_keepsTheOriginalFormat() {
        assertEquals(".png", ArtworkDownloader.extensionOf("https://cdn.test/art/piece.png"));
        assertEquals(".webp", ArtworkDownloader.extensionOf("https://cdn.test/art/piece.WEBP"));
    }

    @Test
    public void extensionOf_fallsBackToJpgWhenTheUrlCarriesNone() {
        assertEquals(".jpg", ArtworkDownloader.extensionOf("https://cdn.test/art/piece"));
        assertEquals(".jpg", ArtworkDownloader.extensionOf(null));
        // A dot in the host is not an extension.
        assertEquals(".jpg", ArtworkDownloader.extensionOf("https://cdn.test/art/piece?alt=media"));
    }

    @Test
    public void fileNameFor_buildsAReadableNameFromTheTitle() {
        assertEquals("ArtVerse-Sunset-Over-Kandy.jpg",
                ArtworkDownloader.fileNameFor("Sunset Over Kandy", STORAGE_URL));
    }

    @Test
    public void fileNameFor_stripsCharactersAFileSystemRejects() {
        String fileName = ArtworkDownloader.fileNameFor("Ravana/Sigiriya: #1 *draft*", STORAGE_URL);

        assertFalse(fileName.contains("/"));
        assertFalse(fileName.contains(":"));
        assertFalse(fileName.contains("*"));
        assertTrue(fileName.endsWith(".jpg"));
    }

    @Test
    public void fileNameFor_staysUsableWhenTheTitleIsMissing() {
        assertEquals("ArtVerse-Artwork.jpg", ArtworkDownloader.fileNameFor(null, STORAGE_URL));
        assertEquals("ArtVerse-Artwork.jpg", ArtworkDownloader.fileNameFor("   ", STORAGE_URL));
        // A title of only stripped characters must not leave an empty name.
        assertEquals("ArtVerse-Artwork.png", ArtworkDownloader.fileNameFor("///", "art.png"));
    }

    @Test
    public void fileNameFor_keepsLongTitlesWithinFileSystemLimits() {
        String longTitle = "A".repeat(200);

        String fileName = ArtworkDownloader.fileNameFor(longTitle, STORAGE_URL);

        assertTrue(fileName.length() <= 80);
        assertTrue(fileName.endsWith(".jpg"));
    }

    @Test
    public void mimeTypeFor_matchesTheSavedExtension() {
        assertEquals("image/jpeg", ArtworkDownloader.mimeTypeFor("ArtVerse-Piece.jpg"));
        assertEquals("image/png", ArtworkDownloader.mimeTypeFor("ArtVerse-Piece.png"));
        assertEquals("image/webp", ArtworkDownloader.mimeTypeFor("ArtVerse-Piece.webp"));
        // Unknown extensions are still images - jpeg is the safe default.
        assertEquals("image/jpeg", ArtworkDownloader.mimeTypeFor("ArtVerse-Piece"));
    }
}
