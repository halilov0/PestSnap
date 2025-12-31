package com.halilovindustries.pestsnap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // UI Components
    private PreviewView previewView;
    private View qualityPanel;
    private TextView qualityStatusText;
    private TextView sharpnessText;
    private TextView exposureText;
    private TextView trapDetectedText;
    private ImageView guideFrameOverlay;
    private Button captureButton;
    private ProgressBar sharpnessProgressBar;

    // CameraX
    private ImageCapture imageCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Quality indicators
    private boolean isTrapDetected = false;
    private int sharpnessLevel = 0;
    private String exposureLevel = "Checking...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initializeViews();
        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (allPermissionsGranted()) {
            startCamera();
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTrapDetected && sharpnessLevel >= 70) {
                    capturePhoto();
                } else {
                    Toast.makeText(CameraActivity.this,
                            "Please align trap properly", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        qualityPanel = findViewById(R.id.qualityPanel);
        qualityStatusText = findViewById(R.id.qualityStatusText);
        sharpnessText = findViewById(R.id.sharpnessText);
        exposureText = findViewById(R.id.exposureText);
        trapDetectedText = findViewById(R.id.trapDetectedText);
        guideFrameOverlay = findViewById(R.id.guideFrameOverlay);
        captureButton = findViewById(R.id.captureButton);
        sharpnessProgressBar = findViewById(R.id.sharpnessProgressBar);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error starting camera", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // Camera selector - back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview use case
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        // Image analysis for quality checking
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                analyzeImageQuality(image);
                image.close();
            }
        });

        // Bind to lifecycle
        camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    private void analyzeImageQuality(ImageProxy image) {
        // Simplified quality analysis - you can enhance this
        // For real implementation, use OpenCV or ML Kit

        // Calculate sharpness (Laplacian variance method would go here)
        sharpnessLevel = calculateSharpness(image);

        // Check exposure
        exposureLevel = checkExposure(image);

        // Detect trap (edge detection or ML model would go here)
        isTrapDetected = detectTrap(image);

        // Update UI on main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateQualityUI();
            }
        });
    }

    private int calculateSharpness(ImageProxy image) {
        // Placeholder - implement Laplacian variance or similar
        // For now, return a mock value
        return (int) (Math.random() * 100);
    }

    private String checkExposure(ImageProxy image) {
        // Placeholder - analyze histogram
        return "Optimal";
    }

    private boolean detectTrap(ImageProxy image) {
        // Placeholder - implement edge detection or ML model
        return sharpnessLevel > 60;
    }

    private void updateQualityUI() {
        if (isTrapDetected && sharpnessLevel >= 70) {
            qualityPanel.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            qualityStatusText.setText("✓ Image Quality: PASS");
            captureButton.setEnabled(true);
            captureButton.setAlpha(1.0f);
        } else {
            qualityPanel.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
            qualityStatusText.setText("Align trap within frame");
            captureButton.setEnabled(false);
            captureButton.setAlpha(0.5f);
        }

        trapDetectedText.setText("Trap Detected: " + (isTrapDetected ? "YES" : "NO"));
        sharpnessText.setText("Sharpness: " + sharpnessLevel + "%");
        sharpnessProgressBar.setProgress(sharpnessLevel);
        exposureText.setText("Exposure: " + exposureLevel);
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        // Create file with timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = dateFormat.format(new Date());
        File photoFile = new File(getExternalFilesDir(null),
                "trap_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                saveTrapMetadata(photoFile);
                                Toast.makeText(CameraActivity.this,
                                        "Image captured successfully!", Toast.LENGTH_SHORT).show();
                                finish(); // Return to history screen
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CameraActivity.this,
                                        "Capture failed: " + exception.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void saveTrapMetadata(File photoFile) {
        // Save to local database with GPS location
        // TODO: Implement Room database save
        Log.d(TAG, "Saving trap metadata: " + photoFile.getAbsolutePath());
        if (currentLocation != null) {
            Log.d(TAG, "Location: " + currentLocation.getLatitude() +
                    ", " + currentLocation.getLongitude());
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            Log.d(TAG, "Location acquired: " + location.getLatitude());
                        }
                    });
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
                getLocation();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
