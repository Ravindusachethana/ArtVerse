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
}
  