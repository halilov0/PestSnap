package com.halilovindustries.pestsnap.data.repository;

import android.content.Context;
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
import com.halilovindustries.pestsnap.data.remote.model.UploadResponse; // וודא שיצרת את המחלקה הזו קודם

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

    public TrapRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        trapDao = database.trapDao();
        pestResultDao = database.pestResultDao();
        // ה-ApiClient המעודכן שלנו כבר מכיל את התיקון ל-SSL
        apiService = ApiClient.getApiService();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void saveTrap(Trap trap, TrapCallback callback) {
        executorService.execute(() -> {
            // שים לב: ב-Dao הוספנו REPLACE, אז זה יעבוד גם לעדכון
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
        return getTrapsByStatus(userId, "captured"); // או "ready" תלוי בערך ששמרת
    }

    public LiveData<List<Trap>> getUploadingTraps(int userId) {
        return getTrapsByStatus(userId, "uploading");
    }

    public LiveData<List<Trap>> getQueuedTraps(int userId) {
        return getTrapsByStatus(userId, "uploaded"); // או "queued"
    }

    public LiveData<List<Trap>> getAllTraps(int userId) {
        return trapDao.getAllTrapsByUser(userId);
    }

    // --- הפונקציה המתוקנת להעלאה ---
    public void uploadTrap(Trap trap, String farmerId, TrapUploadCallback callback) {
        executorService.execute(() -> {
            // 1. עדכון סטטוס מקומי
            trap.setStatus("uploading");
            trapDao.updateTrap(trap);

            try {
                File file = new File(trap.getImagePath());
                if (!file.exists()) {
                    handleUploadFailure(trap, "File not found", callback);
                    return;
                }

                // 2. הכנת ה-Multipart Request
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part imageBody = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

                String gpsString = trap.getLatitude() + "," + trap.getLongitude();
                RequestBody gps = RequestBody.create(MediaType.parse("text/plain"), gpsString);

                // המרת הזמן לפורמט קריא
                String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(new Date(trap.getCapturedAt()));
                RequestBody timestamp = RequestBody.create(MediaType.parse("text/plain"), timeString);

                RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), farmerId);

                // 3. ביצוע הקריאה
                Call<UploadResponse> call = apiService.uploadTrap(imageBody, gps, timestamp, userId);

                call.enqueue(new Callback<UploadResponse>() {
                    @Override
                    public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            executorService.execute(() -> {
                                // הצלחה!
                                trap.setStatus("uploaded"); // או "queued"
                                trap.setRemoteId(response.body().getId()); // שמירת ה-ID מהשרת
                                trapDao.updateTrap(trap);
                            });
                            // שים לב: אנחנו מחזירים פה ID, לא תוצאות אנליזה
                            callback.onUploadSuccess(response.body().getId());
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

    // פונקציית עזר לטיפול בכישלון
    private void handleUploadFailure(Trap trap, String errorMsg, TrapUploadCallback callback) {
        executorService.execute(() -> {
            trap.setStatus("captured"); // החזרה לסטטוס שמאפשר ניסיון חוזר
            trapDao.updateTrap(trap);
        });
        callback.onUploadError(errorMsg);
    }

    // פונקציה לשמירת תוצאות (תשמש אותך בהמשך כשתבדוק סטטוס)
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

    // עדכנתי את הממשק שיקבל ID במקום אובייקט אנליזה מלא
    public interface TrapUploadCallback {
        void onUploadSuccess(int remoteId);
        void onUploadError(String error);
    }
}