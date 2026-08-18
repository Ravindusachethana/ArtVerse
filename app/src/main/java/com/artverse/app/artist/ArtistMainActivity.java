package com.artverse.app.artist;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.artverse.app.R;
import com.artverse.app.artist.fragments.ArtistDashboardFragment;
import com.artverse.app.artist.fragments.ArtistOrdersFragment;
import com.artverse.app.artist.fragments.MyArtworkFragment;
import com.artverse.app.artist.fragments.SalesReportFragment;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.InAppNotifier;
import com.artverse.app.utils.PushTokens;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ArtistMainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 71;

    private BottomNavigationView bottomNav;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_main);

        requestNotificationPermissionIfNeeded();

        bottomNav = findViewById(R.id.bottomNav);

//        findViewById(R.id.btnList).setOnClickListener(v ->
//                openFragment(new BlankFragment()));

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                openFragment(new ArtistDashboardFragment());
                return true;
            } else if (id == R.id.nav_my_art) {
                openFragment(new MyArtworkFragment());
                return true;
            } else if (id == R.id.nav_artist_orders) {
                openFragment(new ArtistOrdersFragment());
                return true;
            } else if (id == R.id.nav_sales) {
                openFragment(new SalesReportFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null && !openOrdersIfRequested(getIntent())) {
            openFragment(new ArtistDashboardFragment());
        }
    }

    /**
     * A tapped "new order" push lands here via SplashActivity, which has
     * already verified the session; all that is left is to show the orders.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openOrdersIfRequested(intent);
    }

    private boolean openOrdersIfRequested(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(Constants.EXTRA_OPEN_ORDERS, false)) {
            return false;
        }
        // Consume it so a later config change does not force the tab again.
        intent.removeExtra(Constants.EXTRA_OPEN_ORDERS);
        bottomNav.setSelectedItemId(R.id.nav_artist_orders);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Surfaces "new order received" alerts while the artist uses the app.
        InAppNotifier.start(this);
        // Keeps this device registered for pushes while the app is closed.
        PushTokens.register();
    }

    @Override
    protected void onStop() {
        InAppNotifier.stop();
        super.onStop();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
        }
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
