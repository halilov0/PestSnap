package com.halilovindustries.pestsnap.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.halilovindustries.pestsnap.R;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", MODE_PRIVATE);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // Load saved setting
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            // Add your logout logic here
            requireActivity().finish();
        });

        return view;
    }
}
