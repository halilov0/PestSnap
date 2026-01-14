package com.halilovindustries.pestsnap.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import com.halilovindustries.pestsnap.data.remote.model.AnalysisResponse;
import com.halilovindustries.pestsnap.data.repository.TrapRepository;

import java.util.List;

public class TrapViewModel extends AndroidViewModel {
    private TrapRepository trapRepository;
    private MutableLiveData<String> uploadMessage;
    private MutableLiveData<Boolean> isUploading;

    public TrapViewModel(@NonNull Application application) {
        super(application);
        trapRepository = new TrapRepository(application);
        uploadMessage = new MutableLiveData<>();
        isUploading = new MutableLiveData<>(false);
    }

    public void saveTrap(Trap trap) {
        trapRepository.saveTrap(trap, new TrapRepository.TrapCallback() {
            @Override
            public void onSuccess(Trap trap) {
                uploadMessage.postValue("Trap saved successfully");
            }

            @Override
            public void onError(String error) {
                uploadMessage.postValue("Error: " + error);
            }
        });
    }

    public void uploadTrap(Trap trap, String farmerId) {
        isUploading.setValue(true); // שים לב: setValue עובד רק ב-Main Thread, אם אתה ברקע השתמש ב-postValue

        trapRepository.uploadTrap(trap, farmerId, new TrapRepository.TrapUploadCallback() {

            // --- התיקון כאן: משנים את החתימה לקבלת int ---
            @Override
            public void onUploadSuccess(int serverId) {
                isUploading.postValue(false);
                // בשלב הזה קיבלנו רק אישור שהתמונה עלתה + מזהה. האנליזה תגיע אחר כך.
                uploadMessage.postValue("Upload successful! Trap ID: " + serverId);
            }

            @Override
            public void onUploadError(String error) {
                isUploading.postValue(false);
                uploadMessage.postValue("Error: " + error);
            }
        });
    }

    public LiveData<List<TrapWithResults>> getAllTrapsWithResults(int userId) {
        return trapRepository.getAllTrapsWithResults(userId);
    }

    public LiveData<List<Trap>> getReadyToUploadTraps(int userId) {
        return trapRepository.getTrapsByStatus(userId, "captured");
    }

    public LiveData<List<Trap>> getUploadingTraps(int userId) {
        return trapRepository.getTrapsByStatus(userId, "uploading");
    }

    public LiveData<List<Trap>> getQueuedTraps(int userId) {
        return trapRepository.getTrapsByStatusIn(userId, List.of("uploaded", "analyzed"));
    }

    public LiveData<List<Trap>> getAllTraps(int userId) {
        return trapRepository.getAllTraps(userId);
    }

    public LiveData<String> getUploadMessage() {
        return uploadMessage;
    }

    public LiveData<Boolean> getIsUploading() {
        return isUploading;
    }

    // saves the trap to the database
    public void update(Trap trap) {
    }
}