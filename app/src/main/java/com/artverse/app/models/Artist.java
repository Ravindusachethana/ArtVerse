package com.artverse.app.models;

import java.util.List;
import java.util.Map;

/**
 * Extended profile for artist-role users. Stored in "artists/{uid}",
 * mirrors the "Artist" entity from the ER diagram (ArtistArts -> categories).
 */
public class Artist {
    public String uid;
    public String businessName;
    public String bio;
    public String location;
    public List<String> categories;
    public double totalSales;
    public int totalArtworks;

    /**
     * Admin approval workflow: "pending" | "approved" | "rejected"
     * (see Constants.ARTIST_STATUS_*). Written by the web admin panel.
     * Null on artists registered before this feature - treated as approved.
     */
    public String status;
    public long reviewedAt;
    public String reviewedBy;

    /**
     * Staged profile edit awaiting admin review (keys: name, phone,
     * profileImageUrl, businessName, location, bio, categories). The live
     * profile stays untouched until the admin panel merges it on approval.
     */
    public Map<String, Object> pendingChanges;

    /** True when a profile edit awaits admin review. Static so Firestore ignores it. */
    public static boolean hasPendingEdit(Artist artist) {
        return artist.pendingChanges != null && !artist.pendingChanges.isEmpty();
    }

    public Artist() { }

    public Artist(String uid, String businessName, String bio, String location,
                  List<String> categories) {
        this.uid = uid;
        this.businessName = businessName;
        this.bio = bio;
        this.location = location;
        this.categories = categories;
        this.totalSales = 0;
        this.totalArtworks = 0;
    }
}
