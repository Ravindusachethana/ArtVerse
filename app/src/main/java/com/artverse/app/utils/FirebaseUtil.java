package com.artverse.app.utils;

import com.artverse.app.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Central access point for Firebase services and Firestore collection
 * references, so UI/Activity code never talks to the Firebase SDK directly
 */
public final class FirebaseUtil {

    private FirebaseUtil() { }

    // 10.0.2.2 is how the Android emulator reaches the host machine's localhost.
    // For a physical device, set this to the host laptop's LAN IP.
    private static final String EMULATOR_HOST = "10.0.2.2";
    private static boolean emulatorsConfigured = false;

    /** Redirects Auth/Firestore/Storage to the local emulator when DEMO_MODE is on. */
    public static void configureEmulatorsIfDemo() {
        if (emulatorsConfigured || !BuildConfig.DEMO_MODE) return;
        emulatorsConfigured = true;
        auth().useEmulator(EMULATOR_HOST, 9099);
        db().useEmulator(EMULATOR_HOST, 8080);
        FirebaseStorage.getInstance().useEmulator(EMULATOR_HOST, 9199);
    }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static StorageReference storage() {
        return FirebaseStorage.getInstance().getReference();
    }

    public static String currentUid() {
        return auth().getCurrentUser() != null ? auth().getCurrentUser().getUid() : null;
    }

    public static boolean isLoggedIn() {
        return auth().getCurrentUser() != null;
    }

    // Collection references
    public static CollectionReference usersRef() {
        return db().collection("users");
    }

    public static CollectionReference artistsRef() {
        return db().collection("artists");
    }

    public static CollectionReference artworksRef() {
        return db().collection("artworks");
    }

    public static CollectionReference categoriesRef() {
        return db().collection("categories");
    }

    public static CollectionReference cartRef(String uid) {
        return db().collection("users").document(uid).collection("cart");
    }

    public static CollectionReference ordersRef() {
        return db().collection("orders");
    }

    public static CollectionReference transactionsRef() {
        return db().collection("transactions");
    }

    public static CollectionReference notificationsRef() {
        return db().collection("notifications");
    }

    public static StorageReference artworkImageRef(String artworkId, String fileName) {
        return storage().child("artwork_images/" + artworkId + "/" + fileName);
    }

    public static StorageReference profileImageRef(String uid) {
        return storage().child("profile_images/" + uid + ".jpg");
    }
}
