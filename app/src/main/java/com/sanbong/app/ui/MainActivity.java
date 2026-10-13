package com.sanbong.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sanbong.app.R;

/**
 * Activity chính chứa thanh điều hướng đáy và chuyển đổi màn hình
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();

        if (savedInstanceState == null) {
            int initialTab = getIntent().getIntExtra("initial_tab", R.id.nav_pitches);
            if (initialTab == R.id.nav_my_bookings) {
                navigateToTab(R.id.nav_my_bookings);
            } else {
                loadFragment(new PitchListFragment());
            }
        }
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_pitches) {
                loadFragment(new PitchListFragment());
                return true;
            } else if (itemId == R.id.nav_my_bookings) {
                loadFragment(new MyBookingsFragment());
                return true;
            } else if (itemId == R.id.nav_favorites) {
                loadFragment(new FavoritesFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    public void navigateToTab(int menuItemId) {
        bottomNavigation.setSelectedItemId(menuItemId);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
