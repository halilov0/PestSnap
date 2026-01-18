//package com.halilovindustries.pestsnap;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.halilovindustries.pestsnap.data.model.Trap;
//import com.halilovindustries.pestsnap.data.repository.UserRepository;
//import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;
//
//// --- השינוי הגדול: אימפורטים מהחבילה החדשה data.remote ---
//import com.halilovindustries.pestsnap.data.remote.ApiClient;
//import com.halilovindustries.pestsnap.data.remote.STARdbiApi;
//import com.halilovindustries.pestsnap.data.remote.model.UploadResponse;
//import com.halilovindustries.pestsnap.utils.NetworkUtils;
//
//import java.io.File;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.MediaType;
//import okhttp3.MultipartBody;
//import okhttp3.RequestBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class QueueActivity extends AppCompatActivity {
//
//    private static final String TAG = "QueueActivity";
//
//    private Button backButton, uploadAllButton;
//    private LinearLayout readyToUploadContainer, uploadingContainer, queuedContainer;
//
//    private TrapViewModel trapViewModel;
//    private UserRepository userRepository;
//    private int currentUserId;
//
//    // רשימה מקומית לשמירת המלכודות המוכנות
//    private List<Trap> currentReadyTraps = new ArrayList<>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setContentView(R.layout.activity_queue);
//
//        Log.d(TAG, "onCreate: QueueActivity started");
//
//        initializeViews();
//
//        // Initialize Data Layer
//        userRepository = new UserRepository(this);
//        currentUserId = userRepository.getCurrentUserId();
//        if (currentUserId == -1) currentUserId = 1; // Default for testing
//
//        Log.d(TAG, "onCreate: Current User ID = " + currentUserId);
//
//        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);
//
//        // Start observing database changes
//        observeTraps();
//
//        setupClickListeners();
//    }
//
//    private void initializeViews() {
//        backButton = findViewById(R.id.backButton);
//        uploadAllButton = findViewById(R.id.uploadAllButton);
//        readyToUploadContainer = findViewById(R.id.readyToUploadContainer);
//        uploadingContainer = findViewById(R.id.uploadingContainer);
//        queuedContainer = findViewById(R.id.queuedContainer);
//    }
//
//    private void observeTraps() {
//        // 1. Observe "Ready to Upload" (Status: captured)
//        trapViewModel.getReadyToUploadTraps(currentUserId).observe(this, traps -> {
//            Log.d(TAG, "📥 observeTraps: READY TO UPLOAD changed - Count: " + (traps != null ? traps.size() : 0));
//            if (traps != null) {
//                for (Trap trap : traps) {
//                    Log.d(TAG, "  → READY Trap ID=" + trap.getId() + " Status=" + trap.getStatus() + " Title=" + trap.getTitle());
//                }
//            }
//            this.currentReadyTraps = traps;
//            updateSection(readyToUploadContainer, traps, "ready");
//        });
//
//        // 2. Observe "Uploading" (Status: uploading)
//        trapViewModel.getUploadingTraps(currentUserId).observe(this, traps -> {
//            Log.d(TAG, "📤 observeTraps: UPLOADING changed - Count: " + (traps != null ? traps.size() : 0));
//            if (traps != null) {
//                for (Trap trap : traps) {
//                    Log.d(TAG, "  → UPLOADING Trap ID=" + trap.getId() + " Status=" + trap.getStatus() + " Title=" + trap.getTitle());
//                }
//            }
//            updateSection(uploadingContainer, traps, "uploading");
//        });
//
//        // 3. Observe "Queued/Uploaded" (Status: uploaded/analyzed)
//        trapViewModel.getQueuedTraps(currentUserId).observe(this, traps -> {
//            Log.d(TAG, "✅ observeTraps: QUEUED/UPLOADED changed - Count: " + (traps != null ? traps.size() : 0));
//            if (traps != null) {
//                for (Trap trap : traps) {
//                    Log.d(TAG, "  → QUEUED Trap ID=" + trap.getId() + " Status=" + trap.getStatus() + " RemoteID=" + trap.getRemoteId());
//                }
//            }
//            updateSection(queuedContainer, traps, "queued");
//        });
//    }
//
//    private void updateSection(LinearLayout container, List<Trap> traps, String type) {
//        container.removeAllViews();
//
//        if (traps == null || traps.isEmpty()) {
//            return;
//        }
//
//        for (Trap trap : traps) {
//            View itemView = LayoutInflater.from(this).inflate(R.layout.item_upload_queue, container, false);
//
//            TextView titleText = itemView.findViewById(R.id.uploadTitle);
//            TextView statusText = itemView.findViewById(R.id.uploadStatus);
//            android.widget.ImageView thumbnailView = itemView.findViewById(R.id.uploadThumbnail);
//            Button itemUploadBtn = itemView.findViewById(R.id.btnUpload);
//
//            if (titleText != null) titleText.setText(trap.getTitle());
//
//            // טעינת תמונה
//            String imagePath = trap.getImagePath();
//            if (thumbnailView != null && imagePath != null) {
//                java.io.File imgFile = new java.io.File(imagePath);
//                if (imgFile.exists()) {
//                    try {
//                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
//                        options.inSampleSize = 8;
//                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
//                        if (bitmap != null) {
//                            thumbnailView.setImageBitmap(bitmap);
//                            thumbnailView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            if (statusText != null) {
//                String sizeStr = String.format(Locale.US, "%.1f MB", trap.getImageSize());
//                if (type.equals("ready")) {
//                    statusText.setText("Ready • " + sizeStr);
//                } else if (type.equals("uploading")) {
//                    statusText.setText("Uploading...");
//                } else {
//                    // כאן משתמשים ב-getRemoteId (או getServerId תלוי במודל שלך)
//                    statusText.setText("Uploaded • ID: " + trap.getRemoteId());
//                }
//            }
//
//            // הגדרת כפתור העלאה בודד
//            if (itemUploadBtn != null) {
//                if (type.equals("ready")) {
//                    itemUploadBtn.setVisibility(View.VISIBLE);
//                    itemUploadBtn.setOnClickListener(v -> {
//                        uploadTrapToServer(trap);
//                    });
//                } else {
//                    itemUploadBtn.setVisibility(View.GONE);
//                }
//            }
//
//            container.addView(itemView);
//        }
//    }
//
//    /**
//     * פונקציה המבצעת את הקריאה ל-API החדש
//     * 🔧 FIXED: Now properly updates status to "uploading" before API call
//     * and reverts to "captured" on failure
//     */
//    private void uploadTrapToServer(Trap trap) {
//        Log.d(TAG, "🚀 uploadTrapToServer: START - Trap ID=" + trap.getId() + " Title=" + trap.getTitle() + " CurrentStatus=" + trap.getStatus());
//
//        File file = new File(trap.getImagePath());
//        if (!file.exists()) {
//            Log.e(TAG, "❌ uploadTrapToServer: File not found at path: " + trap.getImagePath());
//            return;
//        }
//        // ✅ CHECK INTERNET CONNECTIVITY FIRST
//        if (!NetworkUtils.isNetworkAvailable(this)) {
//            Log.e(TAG, "❌ No internet connection - Upload blocked");
//            Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show();
//            return;
//        }
//        Log.d(TAG, "✅ Internet connection verified");
//
//        Log.d(TAG, "📁 uploadTrapToServer: File exists, size=" + (file.length() / 1024) + "KB");
//
//        // ✅ FIX: Update status to "uploading" BEFORE making the API call
//        Log.d(TAG, "🔄 uploadTrapToServer: Changing status to 'uploading' for Trap ID=" + trap.getId());
//        trap.setStatus("uploading");
//        trapViewModel.update(trap);
//        Log.d(TAG, "💾 uploadTrapToServer: Database update called with status='uploading'");
//
//        // 1. הכנת התמונה (MultipartBody.Part)
//        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
//        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
//
//        // 2. הכנת שאר הנתונים (RequestBody)
//        String gpsValue = trap.getLatitude() + "," + trap.getLongitude();
//        RequestBody gps = RequestBody.create(MediaType.parse("text/plain"), gpsValue);
//
//        // פורמט זמן שהשרת מצפה לו
//        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(trap.getCapturedAt()));
//        RequestBody datetime = RequestBody.create(MediaType.parse("text/plain"), timeString);
//
//        RequestBody user = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));
//
//        Log.d(TAG, "📡 uploadTrapToServer: Making API call to server...");
//
//        // 3. ביצוע הקריאה עם ApiClient ו-STARdbiApi החדשים
//        STARdbiApi apiService = ApiClient.getApiService();
//        Call<UploadResponse> call = apiService.uploadTrap(body, gps, datetime, user);
//
//        call.enqueue(new Callback<UploadResponse>() {
//            @Override
//            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
//                Log.d(TAG, "📨 onResponse: Received response - Code=" + response.code() + " Success=" + response.isSuccessful());
//
//                if (response.isSuccessful() && response.body() != null) {
//                    int remoteId = response.body().getId();
//                    Log.d(TAG, "✅ onResponse: SUCCESS - RemoteID=" + remoteId);
//
//                    // עדכון המודל המקומי - מעבר ל"uploaded"
//                    Log.d(TAG, "🔄 onResponse: Changing status to 'uploaded' for Trap ID=" + trap.getId());
//                    trap.setRemoteId(remoteId);
//                    trap.setStatus("uploaded");
//                    trapViewModel.update(trap);
//                    Log.d(TAG, "💾 onResponse: Database update called with status='uploaded' remoteId=" + remoteId);
//
//                } else {
//                    // ✅ FIX: Revert to "captured" on HTTP error
//                    Log.e(TAG, "❌ onResponse: HTTP ERROR - Code=" + response.code() + " Message=" + response.message());
//                    Log.d(TAG, "🔄 onResponse: REVERTING status to 'captured' for Trap ID=" + trap.getId());
//
//                    trap.setStatus("captured");
//                    trapViewModel.update(trap);
//                    Log.d(TAG, "💾 onResponse: Database update called with status='captured' (revert)");
//
//                    Toast.makeText(QueueActivity.this, "Upload Failed: " + response.code(), Toast.LENGTH_LONG).show();
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
//                Log.e(TAG, "❌ onFailure: NETWORK ERROR - " + t.getMessage());
//                t.printStackTrace();
//
//                // ✅ FIX: Revert to "captured" on network error
//                Log.d(TAG, "🔄 onFailure: REVERTING status to 'captured' for Trap ID=" + trap.getId());
//                trap.setStatus("captured");
//                trapViewModel.update(trap);
//                Log.d(TAG, "💾 onFailure: Database update called with status='captured' (revert)");
//
//                Toast.makeText(QueueActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        Log.d(TAG, "⏳ uploadTrapToServer: API call enqueued, waiting for response...");
//    }
//
//    private void setupClickListeners() {
//        backButton.setOnClickListener(v -> finish());
//
//        uploadAllButton.setOnClickListener(v -> {
//            Log.d(TAG, "🔘 uploadAllButton clicked - Ready traps count: " + currentReadyTraps.size());
//
//            if (currentReadyTraps.isEmpty()) {
//                Log.d(TAG, "⚠️ No traps to upload");
//                return;
//            }
//
//            // ✅ CHECK INTERNET BEFORE BATCH UPLOAD
//            if (!NetworkUtils.isNetworkAvailable(this)) {
//                Log.e(TAG, "❌ No internet connection - Batch upload blocked");
//                Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show();
//                return;
//            }
//
//            Log.d(TAG, "📤 Starting batch upload of " + currentReadyTraps.size() + " traps");
//            for (Trap trap : currentReadyTraps) {
//                uploadTrapToServer(trap);
//            }
//        });
//    }
//}
