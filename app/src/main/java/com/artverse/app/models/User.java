package com.artverse.app.models;

/**
 * Base user profile shared by both Customer and Artist roles.
 * Stored in Firestore collection "users/{uid}".
 */
public class User {
    public String uid;
    public String name;
    public String email;
    public String phone;
    public String address;
    public String profileImageUrl;
    public String role;      // "customer" or "artist"
    public long createdAt;

    public User() { }

    public User(String uid, String name, String email, String phone, String address,
                String profileImageUrl, String role, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.createdAt = createdAt;
    }
}
