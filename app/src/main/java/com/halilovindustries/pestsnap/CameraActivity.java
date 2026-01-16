package com.halilovindustries.pestsnap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

// *** הוספנו את ה-Import החסר הזה: ***
import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.repository.TrapRepository;
import com.halilovindustries.pestsnap.data.repository.UserRepository;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private PreviewView previewView;
    private ImageButton captureButton;
    private Button btnBack;
    private TextView qualityDebugText;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    private int sharpnessLevel = 0;
    private boolean isTrapDetected = false;

    private TrapRepository trapRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_camera);

        initializeViews();

        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());


        if (allPermissionsGranted()) {
            startCamera();
            getLocation();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        trapRepository = new TrapRepository(getApplicationContext());
        userRepository = new UserRepository(getApplicationContext());
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        // CHANGE THIS LINE - was Button, now ImageButton
        ImageButton btnBack = findViewById(R.id.btnBack);
        //btnBack = findViewById(R.id.btnBack);
        qualityDebugText = findViewById(R.id.qualityDebugText);
        captureButton.setOnClickListener(v -> capturePhoto());

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        // REMOVE OR COMMENT OUT THESE LINES:
        // ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        //         .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        //         .build();
        // imageAnalysis.setAnalyzer(cameraExecutor, image -> {
        //     analyzeImageQuality(image);
        //     image.close();
        // });

        try {
            // Bind without imageAnalysis
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }


    private void analyzeImageQuality(ImageProxy image) {
        // Mock logic
        sharpnessLevel = (int) (Math.random() * 100); 
        isTrapDetected = sharpnessLevel > 40; 
        runOnUiThread(this::updateQualityUI);
    }

    private void updateQualityUI() {
        String statusText = "Sharpness: " + sharpnessLevel + "% | Ready: YES";
        qualityDebugText.setText(statusText);

        // Keep button always enabled and visible
        captureButton.setAlpha(1.0f);
        captureButton.setEnabled(true);
        qualityDebugText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
    }


    private void capturePhoto() {
        if (imageCapture == null) return;

        captureButton.setEnabled(false);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = dateFormat.format(new Date());
        File photoFile = new File(getExternalFilesDir(null), "trap_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        runOnUiThread(() -> {
                            captureButton.setEnabled(true);
                            
                            // *** CHANGE START: Navigate to Quality Check ***
                            Intent intent = new Intent(CameraActivity.this, QualityCheckActivity.class);
                            
                            // Pass data
                            intent.putExtra("imagePath", photoFile.getAbsolutePath());
                            intent.putExtra("sharpness", sharpnessLevel);
                            intent.putExtra("isTrapDetected", isTrapDetected);
                            if (currentLocation != null) {
                                intent.putExtra("lat", currentLocation.getLatitude());
                                intent.putExtra("lng", currentLocation.getLongitude());
                            }
                            
                            startActivity(intent);
                            // *** CHANGE END ***
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(() -> captureButton.setEnabled(true));
                    }
                });
    }

    // private void saveTrapMetadata(File photoFile) {
    //     int userId = userRepository.getCurrentUserId();
    //     if (userId == -1) userId = 1; 

    //     double lat = 0.0;
    //     double lng = 0.0;
    //     if (currentLocation != null) {
    //         lat = currentLocation.getLatitude();
    //         lng = currentLocation.getLongitude();
    //     }

    //     float fileSizeMB = photoFile.length() / (1024f * 1024f);

    //     Trap newTrap = new Trap(
    //             userId,
    //             "Trap #" + System.currentTimeMillis() % 1000, 
    //             photoFile.getAbsolutePath(),
    //             lat,
    //             lng,
    //             fileSizeMB,
    //             sharpnessLevel, 
    //             isTrapDetected  
    //     );

    //     trapRepository.saveTrap(newTrap, new TrapRepository.TrapCallback() {
    //         @Override
    //         public void onSuccess(Trap trap) {
    //             Log.d(TAG, "Trap saved to DB with ID: " + trap.getId());
    //         }

    //         @Override
    //         public void onError(String error) {
    //             Log.e(TAG, "Failed to save trap to DB: " + error);
    //         }
    //     });
    // }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) currentLocation = location;
            });
        }
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!checkPermission(permission)) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
                getLocation();
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}