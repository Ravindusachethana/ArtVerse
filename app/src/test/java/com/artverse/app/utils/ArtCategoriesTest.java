package com.artverse.app.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regression guard for the shared category list that seeds Firestore and
 * builds the filter/category chips across the app (Chapter 5, Unit Testing -
 * module: ArtCategories).
 */
public class ArtCategoriesTest {

    @Test
    public void defaultCategories_isNotEmpty() {
        assertFalse(ArtCategories.DEFAULT.isEmpty());
    }

    @Test
    public void defaultCategories_containsCoreCategories() {
        assertTrue(ArtCategories.DEFAULT.contains("Painting"));
        assertTrue(ArtCategories.DEFAULT.contains("Sculpture"));
        assertTrue(ArtCategories.DEFAULT.contains("Photography"));
    }

    @Test
    public void defaultCategories_hasNoDuplicates() {
        long distinctCount = ArtCategories.DEFAULT.stream().distinct().count();
        assertTrue(distinctCount == ArtCategories.DEFAULT.size());
    }

    @Test
    public void digitalCategories_areAllOfferedToArtists() {
        // A digital category the artist can never pick would be dead code.
        assertTrue(ArtCategories.DEFAULT.containsAll(ArtCategories.DIGITAL));
    }

    @Test
    public void isDigital_trueForFileDeliveredCategories() {
        assertTrue(ArtCategories.isDigital("Digital Art"));
        assertTrue(ArtCategories.isDigital("Photography"));
    }

    @Test
    public void isDigital_falseForPhysicalCategories() {
        assertFalse(ArtCategories.isDigital("Painting"));
        assertFalse(ArtCategories.isDigital("Sculpture"));
        assertFalse(ArtCategories.isDigital("Ceramics"));
    }

    @Test
    public void isDigital_ignoresCaseAndSurroundingSpace() {
        assertTrue(ArtCategories.isDigital("  digital art "));
        assertTrue(ArtCategories.isDigital("PHOTOGRAPHY"));
    }

    @Test
    public void isDigital_falseWhenCategoryIsMissing() {
        // Order lines written before the category was recorded carry null.
        assertFalse(ArtCategories.isDigital(null));
        assertFalse(ArtCategories.isDigital(""));
    }

    @Test
    public void reproducibleCategories_areAllOfferedToArtists() {
        assertTrue(ArtCategories.DEFAULT.containsAll(ArtCategories.REPRODUCIBLE));
    }

    @Test
    public void isReproducible_trueForCategoriesRemadeToOrder() {
        assertTrue(ArtCategories.isReproducible("Sculpture"));
        assertTrue(ArtCategories.isReproducible("Ceramics"));
        assertTrue(ArtCategories.isReproducible("Printmaking"));
    }

    @Test
    public void isReproducible_falseForOneTimeOriginals() {
        // Painting and Drawing cannot be recreated exactly, so they sell as one.
        assertFalse(ArtCategories.isReproducible("Painting"));
        assertFalse(ArtCategories.isReproducible("Drawing"));
        assertFalse(ArtCategories.isReproducible("Mixed Media"));
    }

    @Test
    public void isReproducible_falseForDigitalCategories() {
        // A digital file is a single copy, never sold by quantity.
        assertFalse(ArtCategories.isReproducible("Digital Art"));
        assertFalse(ArtCategories.isReproducible("Photography"));
    }

    @Test
    public void isReproducible_ignoresCaseAndSurroundingSpace() {
        assertTrue(ArtCategories.isReproducible("  sculpture "));
        assertTrue(ArtCategories.isReproducible("CERAMICS"));
    }

    @Test
    public void isReproducible_falseWhenCategoryIsMissing() {
        assertFalse(ArtCategories.isReproducible(null));
        assertFalse(ArtCategories.isReproducible(""));
    }

    @Test
    public void digitalAndReproducible_neverOverlap() {
        // A category is at most one of the two - the stepper rule depends on it.
        for (String digital : ArtCategories.DIGITAL) {
            assertFalse(ArtCategories.isReproducible(digital));
        }
        for (String reproducible : ArtCategories.REPRODUCIBLE) {
            assertFalse(ArtCategories.isDigital(reproducible));
        }
    }

    @Test
    public void oneOfAKind_coversPhysicalOriginals() {
        assertTrue(ArtCategories.isOneOfAKind("Painting"));
        assertTrue(ArtCategories.isOneOfAKind("Drawing"));
        assertTrue(ArtCategories.isOneOfAKind("Mixed Media"));
    }

    @Test
    public void oneOfAKind_excludesReproducibleAndDigital() {
        // Only these retire on sale; a reproducible or digital piece stays listed.
        assertFalse(ArtCategories.isOneOfAKind("Sculpture"));
        assertFalse(ArtCategories.isOneOfAKind("Ceramics"));
        assertFalse(ArtCategories.isOneOfAKind("Printmaking"));
        assertFalse(ArtCategories.isOneOfAKind("Digital Art"));
        assertFalse(ArtCategories.isOneOfAKind("Photography"));
    }

    @Test
    public void oneOfAKind_ignoresCaseAndSurroundingSpace() {
        assertTrue(ArtCategories.isOneOfAKind("  painting "));
        assertTrue(ArtCategories.isOneOfAKind("MIXED MEDIA"));
    }

    @Test
    public void oneOfAKind_falseWhenCategoryIsMissing() {
        assertFalse(ArtCategories.isOneOfAKind(null));
        assertFalse(ArtCategories.isOneOfAKind(""));
    }

    @Test
    public void supportsMultipleImages_trueForPhysicalMultiAngleCategories() {
        assertTrue(ArtCategories.supportsMultipleImages("Sculpture"));
        assertTrue(ArtCategories.supportsMultipleImages("Ceramics"));
        assertTrue(ArtCategories.supportsMultipleImages("Printmaking"));
    }

    @Test
    public void supportsMultipleImages_falseForFlatOriginalsAndDigital() {
        // Flat 2D originals and single-file digital pieces keep one image.
        assertFalse(ArtCategories.supportsMultipleImages("Painting"));
        assertFalse(ArtCategories.supportsMultipleImages("Drawing"));
        assertFalse(ArtCategories.supportsMultipleImages("Mixed Media"));
        assertFalse(ArtCategories.supportsMultipleImages("Digital Art"));
        assertFalse(ArtCategories.supportsMultipleImages("Photography"));
    }

    @Test
    public void supportsMultipleImages_ignoresCaseAndMissingCategory() {
        assertTrue(ArtCategories.supportsMultipleImages("  sculpture "));
        assertFalse(ArtCategories.supportsMultipleImages(null));
        assertFalse(ArtCategories.supportsMultipleImages(""));
    }

    @Test
    public void multiImageCategories_areAllOfferedToArtists() {
        assertTrue(ArtCategories.DEFAULT.containsAll(ArtCategories.MULTI_IMAGE));
    }

    @Test
    public void maxImages_leavesRoomForACoverAndExtras() {
        assertTrue(ArtCategories.MAX_IMAGES >= 2);
    }

    @Test
    public void everyDefaultCategory_hasExactlyOneSellingRule() {
        // Each category is digital, reproducible, or one-of-a-kind - never two,
        // and never none - so the stepper and retirement rules stay unambiguous.
        for (String category : ArtCategories.DEFAULT) {
            int rules = (ArtCategories.isDigital(category) ? 1 : 0)
                    + (ArtCategories.isReproducible(category) ? 1 : 0)
                    + (ArtCategories.isOneOfAKind(category) ? 1 : 0);
            assertTrue("exactly one rule for " + category, rules == 1);
        }
    }
}
  