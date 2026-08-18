package com.artverse.app.customer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.artverse.app.R;
import com.artverse.app.customer.fragments.CartFragment;
import com.artverse.app.customer.fragments.HomeFragment;
import com.artverse.app.customer.fragments.CustomerOrdersFragment;
import com.artverse.app.customer.fragments.ProfileFragment;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.InAppNotifier;
import com.artverse.app.utils.PushTokens;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerMainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 71;

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        requestNotificationPermissionIfNeeded();

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                openFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_cart) {
                openFragment(new CartFragment());
                return true;
            } else if (id == R.id.nav_orders) {
                openFragment(new CustomerOrdersFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                openFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null
                && !openCartIfRequested(getIntent())
                && !openOrdersIfRequested(getIntent())) {
            openFragment(new HomeFragment());
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!openCartIfRequested(intent)) openOrdersIfRequested(intent);
    }

    /**
     * A tapped order-update push lands here via SplashActivity, which has
     * already verified the session; all that is left is to show the orders.
     */
    private boolean openOrdersIfRequested(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(Constants.EXTRA_OPEN_ORDERS, false)) {
            return false;
        }
        intent.removeExtra(Constants.EXTRA_OPEN_ORDERS);
        bottomNav.setSelectedItemId(R.id.nav_orders);
        return true;
    }

    /** Selects the Cart tab when asked to; the nav listener swaps the fragment. */
    private boolean openCartIfRequested(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(Constants.EXTRA_OPEN_CART, false)) {
            return false;
        }

        intent.removeExtra(Constants.EXTRA_OPEN_CART);
        bottomNav.setSelectedItemId(R.id.nav_cart);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Surfaces "order accepted / rejected" alerts while the customer uses the app.
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
