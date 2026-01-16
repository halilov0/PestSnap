package com.halilovindustries.pestsnap.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.halilovindustries.pestsnap.LoginActivity;
import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.viewmodel.AuthViewModel;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchDarkMode;
    private Button btnLogout;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Initialize Views
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Load saved dark mode preference - FIXED: Using consistent key names
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPref.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        // Dark Mode Toggle - FIXED: Using ternary operator for conciseness
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            // Save the preference
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply();

            // Apply theme using ternary operator (more concise)
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Logout Logic
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Clear authentication state
                authViewModel.logout();

                // Show toast
//                if (getContext() != null) {
//                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
//                }

                // Navigate to LoginActivity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Finish current activity
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        return view;
    }
}