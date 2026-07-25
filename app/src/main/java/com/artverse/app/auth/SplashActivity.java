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
import com.artverse.app.models.Artist;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;

/**
 * Entry point, and the single authority on where a launch may land.
 *
 * It routes on the cached session (customer vs artist, approved vs pending),
 * and it is also where a tapped push notification arrives - the notification
 * carries only *where* the user wanted to go, never permission to go there.
 * Before honouring a deep link this screen verifies, in order:
 *
 *   1. Somebody is signed in - otherwise the link is parked and replayed
 *      after a successful login (see LoginActivity).
 *   2. The signed-in account is the one the alert was addressed to - a handset
 *      later used by a different account falls back to that account's own home
 *      instead of opening someone else's orders.
 *   3. An artist has actually been approved - an unapproved artist still lands
 *      in the read-only waiting room.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 900);
    }

    private void routeNext() {
        String route = getIntent().getStringExtra(Constants.EXTRA_ROUTE);
        String orderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);
        String targetUid = getIntent().getStringExtra(Constants.EXTRA_TARGET_UID);

        // (1) Signed out: park the deep link and replay it after login.
        if (!FirebaseUtil.isLoggedIn()) {
            Intent login = new Intent(this, LoginActivity.class);
            if (route != null) {
                login.putExtra(Constants.EXTRA_ROUTE, route);
                login.putExtra(Constants.EXTRA_ORDER_ID, orderId);
                login.putExtra(Constants.EXTRA_TARGET_UID, targetUid);
            }
            go(login);
            return;
        }

        // (2) Alert addressed to another account: ignore it, keep the session.
        boolean openOrders = Constants.ROUTE_ORDERS.equals(route)
                && (targetUid == null || targetUid.equals(FirebaseUtil.currentUid()));

        SessionManager session = new SessionManager(this);
        if (session.getRole() == null) {
            // Session cache was cleared (or this is a restored install) - the
            // auth state is still valid, so recover the role before routing.
            recoverSessionThenRoute(session, openOrders, orderId);
            return;
        }
        routeHome(session, openOrders, orderId);
    }

    /** (3) Role/approval decide the destination; the deep link only picks the tab. */
    private void routeHome(SessionManager session, boolean openOrders, String orderId) {
        Intent intent;
        if (session.isArtist()) {
            if (!session.isArtistApproved()) {
                // Unapproved artists have no orders screen to open yet.
                go(new Intent(this, ArtistPendingActivity.class));
                return;
            }
            intent = new Intent(this, ArtistMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }

        if (openOrders) {
            intent.putExtra(Constants.EXTRA_OPEN_ORDERS, true);
            if (orderId != null) intent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
        }
        go(intent);
    }

    /** Rebuilds the cached session from Firestore, then routes as usual. */
    private void recoverSessionThenRoute(SessionManager session, boolean openOrders, String orderId) {
        String uid = FirebaseUtil.currentUid();
        FirebaseUtil.usersRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        go(new Intent(this, LoginActivity.class));
                        return;
                    }
                    session.saveSession(uid, user.role, user.name);

                    if (!Constants.ROLE_ARTIST.equals(user.role)) {
                        routeHome(session, openOrders, orderId);
                        return;
                    }
                    FirebaseUtil.artistsRef().document(uid).get()
                            .addOnSuccessListener(artistDoc -> {
                                Artist artist = artistDoc.toObject(Artist.class);
                                session.saveArtistStatus(artist != null ? artist.status : null);
                                routeHome(session, openOrders, orderId);
                            })
                            // Fail closed: without a readable status the artist
                            // waits on the pending screen, which listens for it.
                            .addOnFailureListener(e -> go(new Intent(this, ArtistPendingActivity.class)));
                })
                .addOnFailureListener(e -> go(new Intent(this, LoginActivity.class)));
    }

    private void go(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
