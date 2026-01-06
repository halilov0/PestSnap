package com.halilovindustries.pestsnap.data.repository;

import android.content.Context;
import android.util.Base64;

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
import com.halilovindustries.pestsnap.data.remote.model.UploadRequest;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        apiService = ApiClient.getApiService();
        executorService = Executors.newSingleThreadExecutor();
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
            long trapId = trapDao.insertTrap(trap);
            if (trapId > 0) {
                trap.setId((int) trapId);
            }
        });

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


    public LiveData<List<Trap>> getAllTraps(int userId) {
        return trapDao.getAllTrapsByUser(userId);
    }

    public void uploadTrap(Trap trap, String farmerId, TrapUploadCallback callback) {
        executorService.execute(() -> {
                // Update status to uploading
                trap.setStatus("uploading");
                trapDao.updateTrap(trap);
            try {
                // Convert image to Base64
                String imageBase64 = encodeImageToBase64(trap.getImagePath());

                // Create upload request
                UploadRequest request = new UploadRequest(
                        farmerId,
                        trap.getLatitude(),
                        trap.getLongitude(),
                        trap.getCapturedAt(),
                        imageBase64
                );

                // Upload to STARdbi
                Call<AnalysisResponse> call = apiService.uploadTrap(request);
                call.enqueue(new Callback<AnalysisResponse>() {
                    @Override
                    public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            executorService.execute(() -> {
                                trap.setStatus("analyzed");
                                trapDao.updateTrap(trap);
                                saveAnalysisResults(trap.getId(), response.body());
                            });
                            callback.onUploadSuccess(response.body());
                        } else {
                            executorService.execute(() -> {
                             //   trap.setStatus("captured");
                                trapDao.updateTrap(trap);
                            });
                            callback.onUploadError("Upload failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                        executorService.execute(() -> {
                          //  trap.setStatus("captured");
                            trapDao.updateTrap(trap);
                        });
                        callback.onUploadError("Network error: " + t.getMessage());
                    }
                });

            } catch (Exception e) {
                executorService.execute(() -> {
                    trap.setStatus("captured");
                    trapDao.updateTrap(trap);
                }); callback.onUploadError(e.getMessage());
            }
        });
    }

    private void saveAnalysisResults(int trapId, AnalysisResponse response) {
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
    }

    private String encodeImageToBase64(String imagePath) throws Exception {
        File file = new File(imagePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public interface TrapCallback {
        void onSuccess(Trap trap);
        void onError(String error);
    }

    public interface TrapUploadCallback {
        void onUploadSuccess(AnalysisResponse response);
        void onUploadError(String error);
    }
}