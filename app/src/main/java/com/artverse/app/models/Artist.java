package com.artverse.app.models;

import java.util.List;

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
