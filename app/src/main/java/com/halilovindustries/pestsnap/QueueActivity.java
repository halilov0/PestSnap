package com.halilovindustries.pestsnap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.data.repository.TrapRepository;

import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

import java.util.List;
import java.util.Locale;

public class QueueActivity extends AppCompatActivity {

    private Button backButton, uploadAllButton;
    private LinearLayout readyToUploadContainer, uploadingContainer, queuedContainer;
    
    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        initializeViews();
        
        // Initialize Data Layer
        userRepository = new UserRepository(this);
        currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == -1) currentUserId = 1; // Default for testing

        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);

        // Start observing database changes
        observeTraps();
        
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        uploadAllButton = findViewById(R.id.uploadAllButton);
        readyToUploadContainer = findViewById(R.id.readyToUploadContainer);
        uploadingContainer = findViewById(R.id.uploadingContainer);
        queuedContainer = findViewById(R.id.queuedContainer);
    }

    private void observeTraps() {
        // 1. Observe "Ready to Upload" (Status: captured)
        trapViewModel.getReadyToUploadTraps(currentUserId).observe(this, traps -> {
            updateSection(readyToUploadContainer, traps, "ready");
        });

        // 2. Observe "Uploading" (Status: uploading)
        trapViewModel.getUploadingTraps(currentUserId).observe(this, traps -> {
            updateSection(uploadingContainer, traps, "uploading");
        });

        // 3. Observe "Queued/Uploaded" (Status: uploaded/analyzed)
        // Note: You might want to filter this further based on your specific logic
        trapViewModel.getQueuedTraps(currentUserId).observe(this, traps -> {
            updateSection(queuedContainer, traps, "queued");
        });
    }
    
    private void updateSection(LinearLayout container, List<Trap> traps, String type) {
        container.removeAllViews(); 

        if (traps == null || traps.isEmpty()) {
            android.util.Log.d("QueueDebug", "No traps found for section: " + type);
            return;
        }

        for (Trap trap : traps) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_upload_queue, container, false);

            TextView titleText = itemView.findViewById(R.id.uploadTitle);
            TextView statusText = itemView.findViewById(R.id.uploadStatus);
            android.widget.ImageView thumbnailView = itemView.findViewById(R.id.uploadThumbnail);

            if (titleText != null) titleText.setText(trap.getTitle());
            
            // --- תחילת דיבאג ---
            String imagePath = trap.getImagePath();
            android.util.Log.d("QueueDebug", "--------------------------------------------------");
            android.util.Log.d("QueueDebug", "Checking Trap: " + trap.getTitle());
            android.util.Log.d("QueueDebug", "Path from DB: " + imagePath);

            if (thumbnailView != null && imagePath != null) {
                java.io.File imgFile = new java.io.File(imagePath);
                
                // בדיקה 1: האם הקובץ קיים?
                boolean exists = imgFile.exists();
                android.util.Log.d("QueueDebug", "File exists? " + exists);
                
                if (exists) {
                    android.util.Log.d("QueueDebug", "File size: " + imgFile.length() + " bytes");
                    try {
                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = 8; 

                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        
                        // בדיקה 2: האם הביטמפ נוצר?
                        if (bitmap != null) {
                            android.util.Log.d("QueueDebug", "Bitmap created successfully! Width: " + bitmap.getWidth());
                            thumbnailView.setImageBitmap(bitmap);
                            thumbnailView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        } else {
                            android.util.Log.e("QueueDebug", "ERROR: Bitmap is NULL (decoding failed)");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("QueueDebug", "EXCEPTION: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    android.util.Log.e("QueueDebug", "ERROR: File does NOT exist at path!");
                }
            } else {
                android.util.Log.e("QueueDebug", "View or Path is null. View: " + thumbnailView + ", Path: " + imagePath);
            }
            // --- סוף דיבאג ---

            if (statusText != null) {
                String sizeStr = String.format(Locale.US, "%.1f MB", trap.getImageSize());
                if (type.equals("ready")) {
                    statusText.setText("Ready • " + sizeStr);
                } else if (type.equals("uploading")) {
                    statusText.setText("Uploading...");
                } else {
                    statusText.setText("Uploaded • Waiting for analysis");
                }
            }

            Button itemUploadBtn = itemView.findViewById(R.id.btnUpload);
            if (itemUploadBtn != null) {
                if (type.equals("ready")) {
                    itemUploadBtn.setVisibility(View.VISIBLE);
                    itemUploadBtn.setOnClickListener(v -> {
                        Toast.makeText(this, "Starting upload for " + trap.getTitle(), Toast.LENGTH_SHORT).show();
                        trapViewModel.uploadTrap(trap, String.valueOf(currentUserId));
                    });
                } else {
                    itemUploadBtn.setVisibility(View.GONE);
                }
            }

            container.addView(itemView);
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        uploadAllButton.setOnClickListener(v -> {
            // Logic to upload all "ready" traps
            // For now, just a toast
            Toast.makeText(QueueActivity.this, "Uploading all...", Toast.LENGTH_SHORT).show();
            
            // In real impl: Iterate over readyToUpload list and call uploadTrap for each
        });
    }
}