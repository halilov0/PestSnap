package com.halilovindustries.pestsnap.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.halilovindustries.pestsnap.LoginActivity;
import com.halilovindustries.pestsnap.R;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchDarkMode;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. Initialize Views
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnLogout = view.findViewById(R.id.btnLogout);

        // 2. Load Saved State (Check if user previously set dark mode)
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPref.getBoolean("DARK_MODE", false);
        switchDarkMode.setChecked(isDarkMode);

        // 3. Dark Mode Logic
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("DARK_MODE", isChecked);
            editor.apply();

            // Apply the theme (This will recreate the activity to apply changes)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 4. Logout Logic
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }
}