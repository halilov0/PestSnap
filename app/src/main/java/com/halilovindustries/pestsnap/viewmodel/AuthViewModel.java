package com.halilovindustries.pestsnap.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.halilovindustries.pestsnap.data.model.User;
import com.halilovindustries.pestsnap.data.repository.UserRepository;

public class AuthViewModel extends AndroidViewModel {
    private UserRepository userRepository;
    private MutableLiveData<String> authMessage;
    private MutableLiveData<Boolean> isLoading;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        authMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
    }

    public void registerUser(String firstName, String lastName, String email, String password) {
        isLoading.setValue(true);
        userRepository.registerUser(firstName, lastName, email, password, new UserRepository.RepositoryCallback() {
            @Override
            public void onSuccess(String message) {
                isLoading.postValue(false);
                authMessage.postValue(message);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                authMessage.postValue("Error: " + error);
            }
        });
    }

    public void loginUser(String email, String password) {
        isLoading.setValue(true);
        userRepository.loginUser(email, password, new UserRepository.RepositoryCallback() {
            @Override
            public void onSuccess(String message) {
                isLoading.postValue(false);
                authMessage.postValue(message);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                authMessage.postValue("Error: " + error);
            }
        });
    }

    public void logout() {
        userRepository.logoutUser();
    }

    public LiveData<User> getCurrentUser() {
        return userRepository.getCurrentUser();
    }

    public int getCurrentUserId() {
        return userRepository.getCurrentUserId();
    }

    public LiveData<String> getAuthMessage() {
        return authMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}