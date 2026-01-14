package com.halilovindustries.pestsnap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

// --- השינוי הגדול: אימפורטים מהחבילה החדשה data.remote ---
import com.halilovindustries.pestsnap.data.remote.ApiClient;
import com.halilovindustries.pestsnap.data.remote.STARdbiApi;
import com.halilovindustries.pestsnap.data.remote.model.UploadResponse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QueueActivity extends AppCompatActivity {

    private Button backButton, uploadAllButton;
    private LinearLayout readyToUploadContainer, uploadingContainer, queuedContainer;

    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    // רשימה מקומית לשמירת המלכודות המוכנות
    private List<Trap> currentReadyTraps = new ArrayList<>();

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
            this.currentReadyTraps = traps;
            updateSection(readyToUploadContainer, traps, "ready");
        });

        // 2. Observe "Uploading" (Status: uploading)
        trapViewModel.getUploadingTraps(currentUserId).observe(this, traps -> {
            updateSection(uploadingContainer, traps, "uploading");
        });

        // 3. Observe "Queued/Uploaded" (Status: uploaded/analyzed)
        trapViewModel.getQueuedTraps(currentUserId).observe(this, traps -> {
            updateSection(queuedContainer, traps, "queued");
        });
    }

    private void updateSection(LinearLayout container, List<Trap> traps, String type) {
        container.removeAllViews();

        if (traps == null || traps.isEmpty()) {
            return;
        }

        for (Trap trap : traps) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_upload_queue, container, false);

            TextView titleText = itemView.findViewById(R.id.uploadTitle);
            TextView statusText = itemView.findViewById(R.id.uploadStatus);
            android.widget.ImageView thumbnailView = itemView.findViewById(R.id.uploadThumbnail);
            Button itemUploadBtn = itemView.findViewById(R.id.btnUpload);

            if (titleText != null) titleText.setText(trap.getTitle());

            // טעינת תמונה
            String imagePath = trap.getImagePath();
            if (thumbnailView != null && imagePath != null) {
                java.io.File imgFile = new java.io.File(imagePath);
                if (imgFile.exists()) {
                    try {
                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                        options.inSampleSize = 8;
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        if (bitmap != null) {
                            thumbnailView.setImageBitmap(bitmap);
                            thumbnailView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (statusText != null) {
                String sizeStr = String.format(Locale.US, "%.1f MB", trap.getImageSize());
                if (type.equals("ready")) {
                    statusText.setText("Ready • " + sizeStr);
                } else if (type.equals("uploading")) {
                    statusText.setText("Uploading...");
                } else {
                    // כאן משתמשים ב-getRemoteId (או getServerId תלוי במודל שלך)
                    statusText.setText("Uploaded • ID: " + trap.getRemoteId());
                }
            }

            // הגדרת כפתור העלאה בודד
            if (itemUploadBtn != null) {
                if (type.equals("ready")) {
                    itemUploadBtn.setVisibility(View.VISIBLE);
                    itemUploadBtn.setOnClickListener(v -> {
                        uploadTrapToServer(trap);
                    });
                } else {
                    itemUploadBtn.setVisibility(View.GONE);
                }
            }

            container.addView(itemView);
        }
    }

    /**
     * פונקציה המבצעת את הקריאה ל-API החדש
     */
    private void uploadTrapToServer(Trap trap) {
        File file = new File(trap.getImagePath());
        if (!file.exists()) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading " + trap.getTitle() + "...", Toast.LENGTH_SHORT).show();

        // 1. הכנת התמונה (MultipartBody.Part)
        // המפתח "image" חייב להתאים למה שמוגדר ב-STARdbiApi וב-PDF
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        // 2. הכנת שאר הנתונים (RequestBody)
        // שים לב: אנחנו משתמשים ב-trap.getLatitude() כדי לקבל נתונים אמיתיים
        String gpsValue = trap.getLatitude() + "," + trap.getLongitude();
        RequestBody gps = RequestBody.create(MediaType.parse("text/plain"), gpsValue);

        // פורמט זמן שהשרת מצפה לו
        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(trap.getCapturedAt()));
        RequestBody datetime = RequestBody.create(MediaType.parse("text/plain"), timeString);

        RequestBody user = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));

        // 3. ביצוע הקריאה עם ApiClient ו-STARdbiApi החדשים
        STARdbiApi apiService = ApiClient.getApiService();
        Call<UploadResponse> call = apiService.uploadTrap(body, gps, datetime, user);

        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int remoteId = response.body().getId();

                    // עדכון המודל המקומי
                    trap.setRemoteId(remoteId); // וודא שיש לך setRemoteId במודל Trap
                    trap.setStatus("uploaded");

                    // שמירה במסד הנתונים
                    trapViewModel.update(trap);

                    Toast.makeText(QueueActivity.this, "Upload Success! ID: " + remoteId, Toast.LENGTH_SHORT).show();
                } else {
                    // כישלון (למשל 400 או 404 אם השרת לא מוכן)
                    Toast.makeText(QueueActivity.this, "Upload Failed: " + response.code(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("UploadError", "Code: " + response.code() + " Msg: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                Toast.makeText(QueueActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        uploadAllButton.setOnClickListener(v -> {
            if (currentReadyTraps.isEmpty()) {
                Toast.makeText(this, "Nothing to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Uploading " + currentReadyTraps.size() + " items...", Toast.LENGTH_SHORT).show();

            for (Trap trap : currentReadyTraps) {
                uploadTrapToServer(trap);
            }
        });
    }
}