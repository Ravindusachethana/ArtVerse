package com.artverse.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.artist.ArtistMainActivity;
import com.artverse.app.artist.ArtistPendingActivity;
import com.artverse.app.customer.CustomerMainActivity;
import com.artverse.app.R;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;

/**
 * Entry point. Routes to the correct home screen based on whether the
 * user is already authenticated, and if so, whether they are a
 * customer or an artist (cached locally via SessionManager to avoid an
 * extra Firestore read on every launch).
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 900);
    }

    private void routeNext() {
        SessionManager session = new SessionManager(this);
        Intent intent;

        if (FirebaseUtil.isLoggedIn() && session.getRole() != null) {
            if (session.isArtist()) {
                // Unapproved artists stay in the read-only waiting room; it
                // listens to Firestore and unlocks the dashboard on approval.
                intent = session.isArtistApproved()
                        ? new Intent(this, ArtistMainActivity.class)
                        : new Intent(this, ArtistPendingActivity.class);
            } else {
                intent = new Intent(this, CustomerMainActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
