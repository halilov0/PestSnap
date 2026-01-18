package com.halilovindustries.pestsnap.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.halilovindustries.pestsnap.data.local.AppDatabase;
import com.halilovindustries.pestsnap.data.local.PestResultDao;
import com.halilovindustries.pestsnap.data.local.TrapDao;
import com.halilovindustries.pestsnap.data.model.PestResult;
import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import com.halilovindustries.pestsnap.data.remote.ApiClient;
import com.halilovindustries.pestsnap.data.remote.STARdbiApi;
import com.halilovindustries.pestsnap.data.remote.model.AnalysisResponse;
import com.halilovindustries.pestsnap.data.remote.model.UploadResponse;
import com.halilovindustries.pestsnap.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrapRepository {
    private TrapDao trapDao;
    private PestResultDao pestResultDao;
    private STARdbiApi apiService;
    private ExecutorService executorService;
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    private Context context;

    public TrapRepository(Context context) {
        this.context = context;
        AppDatabase database = AppDatabase.getInstance(context);
        trapDao = database.trapDao();
        pestResultDao = database.pestResultDao();
        apiService = ApiClient.getApiService();
        executorService = Executors.newSingleThreadExecutor();

        // Start auto-retry service
        startAutoRetryService();
    }

    // 🆕 Auto-retry service that checks every 10 seconds
    private void startAutoRetryService() {
        retryHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    retryFailedUploads();
                }
                retryHandler.postDelayed(this, 10000); // Repeat every 10 seconds
            }
        }, 10000);
    }

    // 🆕 Retry all traps stuck in "uploading" state
    private void retryFailedUploads() {
        executorService.execute(() -> {
            List<Trap> uploadingTraps = trapDao.getTrapsByStatusSync("uploading");

            if (uploadingTraps != null && !uploadingTraps.isEmpty()) {
                android.util.Log.d("TrapRetry", "🔄 Retrying " + uploadingTraps.size() + " uploads");

                for (Trap trap : uploadingTraps) {
                    retryUpload(trap);
                }
            }
        });
    }

    // 🆕 Retry a single upload
    private void retryUpload(Trap trap) {
        try {
            File file = new File(trap.getImagePath());
            if (!file.exists()) {
                android.util.Log.e("TrapRetry", "File not found: " + trap.getImagePath());
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imageBody = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            String gpsString = trap.getLatitude() + "," + trap.getLongitude();
            RequestBody gps = RequestBody.create(MediaType.parse("text/plain"), gpsString);

            String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date(trap.getCapturedAt()));
            RequestBody timestamp = RequestBody.create(MediaType.parse("text/plain"), timeString);

            String farmerId = String.valueOf(trap.getUserId());
            RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), farmerId);

            Call<UploadResponse> call = apiService.uploadTrap(imageBody, gps, timestamp, userId);
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        android.util.Log.d("TrapRetry", "✅ Retry SUCCESS for trap " + trap.getId());

                        int remoteId = response.body().getId();
                        executorService.execute(() -> {
                            trap.setStatus("uploaded");
                            trap.setRemoteId(remoteId);
                            trapDao.updateTrap(trap);

                            startPollingForResults(trap.getId(), remoteId);
                        });
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    android.util.Log.e("TrapRetry", "❌ Retry failed for trap " + trap.getId());
                }
            });

        } catch (Exception e) {
            android.util.Log.e("TrapRetry", "Exception during retry", e);
        }
    }

    public void saveTrap(Trap trap, TrapCallback callback) {
        executorService.execute(() -> {
            long trapId = trapDao.insertTrap(trap);
            if (trapId > 0) {
                trap.setId((int) trapId);
                callback.onSuccess(trap);
            } else {
                callback.onError("Failed to save trap");
            }
        });
    }

    public void saveTrap(Trap trap) {
        executorService.execute(() -> {
            trapDao.insertTrap(trap);
        });
    }

    public void update(Trap trap) {
        executorService.execute(() -> trapDao.updateTrap(trap));
    }

    public LiveData<List<TrapWithResults>> getAllTrapsWithResults(int userId) {
        return trapDao.getTrapsWithResults(userId);
    }

    public LiveData<List<Trap>> getTrapsByStatus(int userId, String status) {
        return trapDao.getTrapsByStatus(userId, status);
    }

    public LiveData<List<Trap>> getTrapsByStatusIn(int userId, List<String> status) {
        return trapDao.getTrapsByStatusIn(userId, status);
    }

    public LiveData<List<Trap>> getReadyToUploadTraps(int userId) {
        return getTrapsByStatus(userId, "captured");
    }

    public LiveData<List<Trap>> getUploadingTraps(int userId) {
        return getTrapsByStatus(userId, "uploading");
    }

    public LiveData<List<Trap>> getQueuedTraps(int userId) {
        return getTrapsByStatus(userId, "uploaded");
    }

    public LiveData<List<Trap>> getAllTraps(int userId) {
        return trapDao.getAllTrapsByUser(userId);
    }

    public void uploadTrap(Trap trap, String farmerId, TrapUploadCallback callback) {
        executorService.execute(() -> {
            // 1. קודם כל מעבירים לסטטוס 'uploading' ומעדכנים ב-DB
            // זה גורם למלכודת לזוז מיידית ב-UI של המשתמש לחלק של ה-Uploading
            trap.setStatus("uploading");
            trapDao.updateTrap(trap);

            android.util.Log.d("TrapUpload", "Status set to 'uploading' for Trap #" + trap.getId());

            // 2. עכשיו בודקים: האם יש בכלל אינטרנט?
            if (!NetworkUtils.isNetworkAvailable(context)) {
                android.util.Log.d("TrapUpload", "⛔ No internet. Trap parked in 'uploading' state. Auto-retry service will handle it later.");
                // אנחנו עוצרים כאן! לא מנסים לשלוח לשרת כדי לא לקבל שגיאה.
                // המלכודת תישאר בסטטוס uploading והשירות האוטומטי (startAutoRetryService) יאסוף אותה כשהאינטרנט יחזור.
                return;
            }

            // 3. יש אינטרנט - ממשיכים להעלאה רגילה
            try {
                File file = new File(trap.getImagePath());
                if (!file.exists()) {
                    handleUploadFailure(trap, "File not found", callback);
                    return;
                }

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part imageBody = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

                String gpsString = trap.getLatitude() + "," + trap.getLongitude();
                RequestBody gps = RequestBody.create(MediaType.parse("text/plain"), gpsString);

                String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(new Date(trap.getCapturedAt()));
                RequestBody timestamp = RequestBody.create(MediaType.parse("text/plain"), timeString);

                RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), farmerId);

                Call<UploadResponse> call = apiService.uploadTrap(imageBody, gps, timestamp, userId);

                call.enqueue(new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int remoteId = response.body().getId();
                            executorService.execute(() -> {
                                trap.setStatus("uploaded");
                                trap.setRemoteId(remoteId);
                                trapDao.updateTrap(trap);
                                startPollingForResults(trap.getId(), remoteId);
                            });
                            if (callback != null) callback.onUploadSuccess(remoteId);
                        } else {
                            handleUploadFailure(trap, "Upload failed: " + response.code(), callback);
                        }
                    }

                    @Override
                    public void onFailure(Call<UploadResponse> call, Throwable t) {
                        handleUploadFailure(trap, "Network error: " + t.getMessage(), callback);
                    }
                });

            } catch (Exception e) {
                handleUploadFailure(trap, e.getMessage(), callback);
            }
        });
    }

    // 🆕 Poll for analysis results
    public void startPollingForResults(int trapId, int remoteId) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable pollTask = new Runnable() {
            int attempts = 0;
            final int MAX_ATTEMPTS = 10; // Poll for 20 seconds max (2s * 10)

            @Override
            public void run() {
                if (attempts >= MAX_ATTEMPTS) {
                    android.util.Log.d("TrapPolling", "Max polling attempts reached for trap " + trapId);
                    return;
                }

                attempts++;
                android.util.Log.d("TrapPolling", "Polling attempt " + attempts + " for remoteId: " + remoteId);

                Call<String> statusCall = apiService.getTrapStatus(remoteId);

                // 🔧 FIX: Store reference to this Runnable
                final Runnable self = this;

                statusCall.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String status = response.body();
                            android.util.Log.d("TrapPolling", "Status response: " + status);

                            if (status.equals("In queue")) {
                                // Still processing, poll again in 2 seconds
                                handler.postDelayed(self, 2000); // ✅ Use 'self'
                            } else {
                                // Results ready! Parse and save
                                parseAndSaveResults(trapId, status);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        android.util.Log.e("TrapPolling", "Polling failed: " + t.getMessage());
                        // Retry after 2 seconds
                        handler.postDelayed(self, 2000); // ✅ Use 'self'
                    }
                });
            }
        };

        // Start polling after 2 seconds
        handler.postDelayed(pollTask, 2000);
    }


    // 🆕 Parse JSON results and save to database
    // בתוך TrapRepository.java

    private void parseAndSaveResults(int trapId, String jsonResponse) {
        executorService.execute(() -> {
            try {
                android.util.Log.d("TrapPolling", "🔍 Processing result for trapId: " + trapId);

                // 1. קודם כל מוודאים שה-JSON תקין ברמה בסיסית
                JSONObject json = new JSONObject(jsonResponse);

                // 2. שולפים את המלכודת
                Trap trap = trapDao.getTrapByIdSync(trapId);

                if (trap != null) {
                    // 3. עדכון הסטטוס הוא הדבר הכי חשוב כדי להוציא אותה מהתור!
                    // נבצע אותו לפני שנתעסק עם המזיקים שעלולים לגרום לקריסה
                    trap.setStatus("analyzed");
                    //trap.setAnalysisRawResult(jsonResponse); // אופציונלי: שמירת ה-JSON הגולמי למקרה של דיבאג
                    trapDao.updateTrap(trap);

                    android.util.Log.d("TrapPolling", "✅ Trap status updated to 'analyzed'. Removing from queue.");

                    // 4. עכשיו מנסים לפענח את המזיקים. גם אם זה נכשל, המלכודת כבר לא בתור.
                    try {
                        JSONArray pests = json.optJSONArray("detectedPests");
                        if (pests != null && pests.length() > 0) {
                            for (int i = 0; i < pests.length(); i++) {
                                JSONObject pest = pests.getJSONObject(i);
                                PestResult result = new PestResult(
                                        trapId,
                                        pest.optString("commonName", "Unknown"), // שימוש ב-optString למניעת קריסה
                                        pest.optString("scientificName", ""),
                                        pest.optInt("count", 1),
                                        (float) pest.optDouble("confidence", 0.0),
                                        json.optString("recommendation", ""),
                                        json.optBoolean("requiresAction", false)
                                );
                                pestResultDao.insertPestResult(result);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("TrapPolling", "⚠️ Error parsing pests details, but trap status is safe.", e);
                    }

                } else {
                    android.util.Log.e("TrapPolling", "❌ Trap not found in DB during result parsing!");
                }

            } catch (Exception e) {
                // זה קורה רק אם ה-JSON עצמו פגום לחלוטין
                android.util.Log.e("TrapPolling", "🔥 Critical Error parsing result JSON", e);
            }
        });
    }


    // 🆕 Modified - DON'T revert to "captured", keep as "uploading"
    private void handleUploadFailure(Trap trap, String errorMsg, TrapUploadCallback callback) {
        android.util.Log.d("TrapUpload", "⏳ Upload failed, keeping in 'uploading' state for retry");
        // Trap stays in "uploading" state - will be retried automatically
        callback.onUploadError(errorMsg);
    }

    public void saveAnalysisResults(int trapId, AnalysisResponse response) {
        executorService.execute(() -> {
            if (response.getDetectedPests() != null) {
                for (AnalysisResponse.DetectedPest pest : response.getDetectedPests()) {
                    PestResult result = new PestResult(
                            trapId,
                            pest.getCommonName(),
                            pest.getScientificName(),
                            pest.getCount(),
                            pest.getConfidence(),
                            response.getRecommendation(),
                            response.isRequiresAction()
                    );
                    pestResultDao.insertPestResult(result);
                }
            }
        });
    }

    public interface TrapCallback {
        void onSuccess(Trap trap);
        void onError(String error);
    }

    public interface TrapUploadCallback {
        void onUploadSuccess(int remoteId);
        void onUploadError(String error);
    }
}
