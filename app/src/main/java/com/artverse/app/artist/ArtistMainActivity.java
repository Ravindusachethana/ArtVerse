package com.artverse.app.artist;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.artverse.app.R;
import com.artverse.app.artist.fragments.ArtistDashboardFragment;
import com.artverse.app.artist.fragments.ArtistOrdersFragment;
import com.artverse.app.artist.fragments.MyArtworkFragment;
import com.artverse.app.artist.fragments.SalesReportFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ArtistMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            openFragment(new ArtistDashboardFragment());
        }

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
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
