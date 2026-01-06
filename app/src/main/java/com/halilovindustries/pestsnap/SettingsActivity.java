package com.halilovindustries.pestsnap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.halilovindustries.pestsnap.viewmodel.AuthViewModel;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchDarkMode;
    private Button btnLogout;
    private AuthViewModel authViewModel;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings); // שים לב: זה Activity שטוען layout של פרגמנט, וודא שזה מכוון

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        initializeViews();
        loadDarkModeSetting();
        setupListeners();
    }

    private void initializeViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadDarkModeSetting() {
        // Load saved preference without triggering listener
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);
    }

    private void setupListeners() {
        // Dark Mode Toggle - with loop prevention
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Only apply if user actually clicked (not programmatic change)
            if (!buttonView.isPressed()) return;

            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            // Don't show toast - activity will recreate anyway
        });

        // Logout Button - LOGIC RESOLVED HERE
        btnLogout.setOnClickListener(v -> {
            // 1. ניקוי נתונים (מגרסת idan)
            // זה החלק הכי חשוב - בגרסת main הפקודה הזו הייתה חסרה
            authViewModel.logout();

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // 2. לאן עוברים? (מגרסת idan)
            // עדיף לעבור ל-LoginActivity כדי שהמשתמש יתחבר מחדש, ולא ל-MainActivity
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            
            // 3. דגלים לניקוי היסטוריה (מגרסת idan)
            // הדגלים האלו מוחקים את כל ההיסטוריה, כך שהמשתמש לא יכול ללחוץ "Back" ולחזור לאפליקציה
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            startActivity(intent);
            finish();
        });
    }
}