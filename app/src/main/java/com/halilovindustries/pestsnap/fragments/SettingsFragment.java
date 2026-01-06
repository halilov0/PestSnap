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

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.halilovindustries.pestsnap.LoginActivity; // וודא שהנתיב נכון
import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.viewmodel.AuthViewModel;

public class SettingsFragment extends Fragment {
    
    // לקחתי את הגדרת המשתנים ברמת המחלקה מגרסת idan כדי שתהיה גישה אליהם בכל הקובץ
    private SwitchMaterial switchDarkMode;
    private Button btnLogout;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. אתחול ה-ViewModel (מגרסת idan)
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 2. אתחול ה-Views (מגרסת idan)
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnLogout = view.findViewById(R.id.btnLogout);

        // 3. טעינת מצב קיים (שילוב של השניים)
        // בחרתי להשתמש במפתחות של idan ("AppSettings"), אך הוספתי שמירה קריטית מ-main בהמשך
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPref.getBoolean("DARK_MODE", false);
        switchDarkMode.setChecked(isDarkMode);

        // 4. לוגיקה של מצב חשוך (שילוב: לוגיקה מ-idan + שמירה מ-main)
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            // תוספת חשובה מגרסת main: שמירת המצב החדש בזיכרון!
            sharedPref.edit().putBoolean("DARK_MODE", isChecked).apply();

            // שינוי הנושא (Theme)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 5. לוגיקה של התנתקות (מגרסת idan - כי היא המלאה והנכונה)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // ניקוי המצב ב-ViewModel
                authViewModel.logout();

                // הוד למשתמש
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                }

                // מעבר למסך התחברות
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // סגירת המסך הנוכחי
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        return view;
    }
}