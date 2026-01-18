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
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        // APPLY SAVED DARK MODE SETTING BEFORE EVERYTHING
//        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
//        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
//        if (isDarkMode) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // LOAD HOME FRAGMENT ON START
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new HomeFragment())
//                    .commit();
//        }
//
//        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
//        FloatingActionButton fabCapture = findViewById(R.id.fabCapture);
//
//        fabCapture.setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, CameraActivity.class));
//        });
//
//        // Bottom Navigation
//        bottomNav.setOnItemSelectedListener(item -> {
//            int itemId = item.getItemId();
//
//            if (itemId == R.id.nav_home) {
//                // Already on home
//                return true;
//
//            } else if (itemId == R.id.nav_results) {
//                startActivity(new Intent(MainActivity.this, ResultsActivity.class));
//                //Toast.makeText(this, "Results coming soon", Toast.LENGTH_SHORT).show();
//                return true;
//
//            } else if (itemId == R.id.nav_queue) {
//                startActivity(new Intent(MainActivity.this, QueueActivity.class));
//                return true;
//
//            } else if (itemId == R.id.nav_settings) {
//                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
//                return true;
//            }
//
//            return false;
//        });
//    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // APPLY SAVED DARK MODE SETTING
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        android.util.Log.e("MainActivity", "===== APP STARTED =====");
        setContentView(R.layout.activity_main);

        // CHANGE: Initialize the FIELD variable, not a local one
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabCapture = findViewById(R.id.fabCapture);

        // Load Home Fragment on start ONLY if no special intent
        if (savedInstanceState == null && getIntent().getStringExtra("open_tab") == null) {
            loadFragment(new HomeFragment());
        }

        // FAB opens Camera (separate activity)
        fabCapture.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraActivity.class));
        });

        // Bottom Navigation - USE FRAGMENTS, NOT ACTIVITIES
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_results) {
                fragment = new ResultsFragment();
            } else if (itemId == R.id.nav_queue) {
                fragment = new QueueFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }

            return false;
        });

        // IMPORTANT: Call this AFTER bottomNavigationView is initialized
        handleIncomingIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        handleIncomingIntent(); // Handle the new intent
    }

    private void handleIncomingIntent() {
        String openTab = getIntent().getStringExtra("open_tab");
        if ("queue".equals(openTab)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_queue);
            loadFragment(new QueueFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}