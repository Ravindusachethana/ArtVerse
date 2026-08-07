package com.artverse.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight local cache of the signed-in user's role, avoiding an extra
 * Firestore read on every app launch just to route Customer vs Artist.
 */
public class SessionManager {

    private static final String PREFS_NAME = "artverse_session";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";
    private static final String KEY_UID = "uid";
    private static final String KEY_ARTIST_STATUS = "artist_status";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String uid, String role, String name) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, name)
                .apply();
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public boolean isArtist() {
        return "artist".equals(getRole());
    }

    /** Caches the artist's admin-approval status ("pending"/"approved"/"rejected"). */
    public void saveArtistStatus(String status) {
        prefs.edit().putString(KEY_ARTIST_STATUS, status).apply();
    }

    public String getArtistStatus() {
        return prefs.getString(KEY_ARTIST_STATUS, null);
    }

    /**
     * True when the artist may use the full artist experience. A null status
     * means the account predates the approval feature (legacy) and keeps access.
     */
    public boolean isArtistApproved() {
        String status = getArtistStatus();
        return status == null || Constants.ARTIST_STATUS_APPROVED.equals(status);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
