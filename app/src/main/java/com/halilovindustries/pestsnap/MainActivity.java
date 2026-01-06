package com.halilovindustries.pestsnap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate; // Import this!
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.halilovindustries.pestsnap.fragments.HomeFragment;
import com.halilovindustries.pestsnap.fragments.ResultsFragment;
import com.halilovindustries.pestsnap.fragments.QueueFragment;
import com.halilovindustries.pestsnap.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        FloatingActionButton fabCapture = findViewById(R.id.fabCapture);

        // FAB - Open Camera
        fabCapture.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraActivity.class));
        });

        // Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on home
                return true;

            } else if (itemId == R.id.nav_results) {
                // TODO: Open Results
                Toast.makeText(this, "Results coming soon", Toast.LENGTH_SHORT).show();
                return true;

            } else if (itemId == R.id.nav_queue) {
                startActivity(new Intent(MainActivity.this, QueueActivity.class));
                return true;

            } else if (itemId == R.id.nav_settings) {
                // TODO: Open Settings
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }


    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabCapture = findViewById(R.id.fabCapture);
        bottomNavigationView.getMenu().findItem(R.id.nav_placeholder).setEnabled(false);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_results) {
                fragment = new ResultsFragment();
            } else if (id == R.id.nav_queue) {
                fragment = new QueueFragment();
            } else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        fabCapture.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}