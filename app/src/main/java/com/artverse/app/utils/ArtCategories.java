package com.artverse.app.utils;

import java.util.Arrays;
import java.util.List;

/** Default artwork categories, used to seed Firestore and to build filter chips. */
public final class ArtCategories {

    private ArtCategories() { }

    public static final List<String> DEFAULT = Arrays.asList(
            "Painting", "Sculpture", "Photography", "Digital Art",
            "Ceramics", "Drawing", "Printmaking", "Mixed Media"
    );
}
