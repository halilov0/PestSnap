package com.halilovindustries.pestsnap;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.repository.TrapRepository;
import com.halilovindustries.pestsnap.data.repository.UserRepository;

import java.io.File;

public class QualityCheckActivity extends AppCompatActivity {

    private Button btnApprove, btnRetake, btnBack;
    private TextView metricSharpness, metricTrapDetected;
    
    // Data passed from Camera
    private String imagePath;
    private int sharpnessScore;
    private boolean isTrapDetected;
    private double latitude;
    private double longitude;

    private TrapRepository trapRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_quality_check);

        // 1. Get Data from Intent
        imagePath = getIntent().getStringExtra("imagePath");
        sharpnessScore = getIntent().getIntExtra("sharpness", 0);
        isTrapDetected = getIntent().getBooleanExtra("isTrapDetected", false);
        latitude = getIntent().getDoubleExtra("lat", 0.0);
        longitude = getIntent().getDoubleExtra("lng", 0.0);

        initializeViews();
        displayMetrics();
        setupRepositories();
        setupClickListeners();
    }

    private void initializeViews() {
        btnApprove = findViewById(R.id.btnApprove);
        btnRetake = findViewById(R.id.btnRetake);
        btnBack = findViewById(R.id.btnBack);
        metricSharpness = findViewById(R.id.metricSharpness);
        metricTrapDetected = findViewById(R.id.metricTrapDetected);
    }

    private void displayMetrics() {
        metricSharpness.setText(sharpnessScore + "%");
        metricTrapDetected.setText(isTrapDetected ? "YES" : "NO");
    }

    private void setupRepositories() {
        trapRepository = new TrapRepository(getApplicationContext());
        userRepository = new UserRepository(getApplicationContext());
    }

    private void setupClickListeners() {
        // APPROVE: Save to DB and go to Queue
        btnApprove.setOnClickListener(v -> saveAndContinue());

        // RETAKE: Delete temp file and go back
        btnRetake.setOnClickListener(v -> {
            deleteTempFile();
            finish(); // Go back to Camera
        });

        btnBack.setOnClickListener(v -> {
            deleteTempFile();
            finish();
        });
    }

    private void saveAndContinue() {
        int userId = userRepository.getCurrentUserId();
        if (userId == -1) userId = 1;

        File file = new File(imagePath);
        float fileSizeMB = file.length() / (1024f * 1024f);

        Trap newTrap = new Trap(
                userId,
                "Trap #" + System.currentTimeMillis() % 1000,
                imagePath,
                latitude,
                longitude,
                fileSizeMB,
                sharpnessScore,
                isTrapDetected
        );

        trapRepository.saveTrap(newTrap, new TrapRepository.TrapCallback() {
            @Override
            public void onSuccess(Trap trap) {
                runOnUiThread(() -> {
                    Toast.makeText(QualityCheckActivity.this, "Trap Saved!", Toast.LENGTH_SHORT).show();
                    // Navigate to MainActivity and open Queue tab
                    Intent intent = new Intent(QualityCheckActivity.this, MainActivity.class);
                    intent.putExtra("open_tab", "queue"); // Tell MainActivity to open Queue tab
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();

                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(QualityCheckActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteTempFile() {
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}