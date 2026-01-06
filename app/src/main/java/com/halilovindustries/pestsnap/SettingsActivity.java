package com.halilovindustries.pestsnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchDarkMode;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupListeners() {
        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this, "Dark mode disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout Button
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

            // Clear user session (add your logic here)
            // userRepository.clearSession();

            // Go back to login or main screen
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
