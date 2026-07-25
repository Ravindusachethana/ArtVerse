package com.artverse.app.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Default artwork categories, used to seed Firestore and to build filter chips. */
public final class ArtCategories {

    private ArtCategories() { }

    public static final List<String> DEFAULT = Arrays.asList(
            "Painting", "Sculpture", "Photography", "Digital Art",
            "Ceramics", "Drawing", "Printmaking", "Mixed Media"
    );

    /**
     * Categories that have no physical piece to ship: the file itself is the
     * artwork, so it is delivered inside the app (see CollectionAccess). A file
     * is a single copy, so these never show a quantity stepper either.
     */
    public static final List<String> DIGITAL = Arrays.asList("Digital Art", "Photography");

    /**
     * Categories whose pieces can be remade to order, so the artist stocks and
     * sells them by quantity. Everything outside this list is a single piece -
     * a one-time original that cannot be recreated exactly (Painting, Drawing,
     * Mixed Media) or a digital file (see DIGITAL) - and has no quantity
     * stepper. Printmaking belongs here: prints are pulled in editions.
     */
    public static final List<String> REPRODUCIBLE = Arrays.asList(
            "Sculpture", "Ceramics", "Printmaking");

    /**
     * Physical originals that exist as a single piece and cannot be recreated
     * exactly, so they are sold once and then retired from the shop (see
     * Artwork.retired). The admin panel mirrors this list in
     * OrdersService.ONE_OF_A_KIND - keep the two in step.
     */
    public static final List<String> ONE_OF_A_KIND = Arrays.asList(
            "Painting", "Drawing", "Mixed Media");

    /**
     * Categories the artist may photograph from several angles, so an Art Lover
     * gets a realistic idea of a physical piece before buying. These are the
     * three-dimensional and textured works (Sculpture, Ceramics, Printmaking)
     * where one photo cannot show the whole piece; flat 2D originals and
     * single-file digital pieces keep one image. Independent of the
     * reproducible/digital axes on purpose, so the set can grow on its own.
     */
    public static final List<String> MULTI_IMAGE = Arrays.asList(
            "Sculpture", "Ceramics", "Printmaking");

    /** Most images any one listing may carry (Firestore imageUrls). */
    public static final int MAX_IMAGES = 5;

    /** True when a listing is delivered as a file rather than shipped. */
    public static boolean isDigital(String categoryName) {
        return matches(categoryName, DIGITAL);
    }

    /** True when the artist may add several photos of the piece, not just one. */
    public static boolean supportsMultipleImages(String categoryName) {
        return matches(categoryName, MULTI_IMAGE);
    }

    /**
     * True for a single physical original (Painting, Drawing, Mixed Media),
     * which is sold once and retired once its delivery completes.
     */
    public static boolean isOneOfAKind(String categoryName) {
        return matches(categoryName, ONE_OF_A_KIND);
    }

    /** True when the artist may sell more than one identical piece - the only
     *  case with a quantity stepper. Physical originals and digital files are
     *  single-copy and return false. */
    public static boolean isReproducible(String categoryName) {
        return matches(categoryName, REPRODUCIBLE);
    }

    private static boolean matches(String categoryName, List<String> categories) {
        if (categoryName == null) return false;
        String normalized = categoryName.trim().toLowerCase(Locale.ROOT);
        for (String category : categories) {
            if (category.toLowerCase(Locale.ROOT).equals(normalized)) return true;
        }
        return false;
    }
}
