package com.halilovindustries.pestsnap.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.halilovindustries.pestsnap.data.local.AppDatabase;
import com.halilovindustries.pestsnap.data.local.UserDao;
import com.halilovindustries.pestsnap.data.model.User;

import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private UserDao userDao;
    private ExecutorService executorService;
    private SharedPreferences sharedPreferences;

    public UserRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();
        sharedPreferences = context.getSharedPreferences("PestSnapPrefs", Context.MODE_PRIVATE);
    }

    public void registerUser(String firstName, String lastName, String email,
                             String password, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                String passwordHash = hashPassword(password);
                User user = new User(firstName, lastName, email, passwordHash);
                long userId = userDao.insertUser(user);

                if (userId > 0) {
                    saveCurrentUserId((int) userId);
                    callback.onSuccess("User registered successfully");
                } else {
                    callback.onError("Registration failed");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void loginUser(String email, String password, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                User user = userDao.getUserByEmail(email);

                if (user != null && verifyPassword(password, user.getPasswordHash())) {
                    saveCurrentUserId(user.getId());
                    callback.onSuccess("Login successful");
                } else {
                    callback.onError("Invalid email or password");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void logoutUser() {
        sharedPreferences.edit().remove("current_user_id").apply();
    }

    public int getCurrentUserId() {
        return sharedPreferences.getInt("current_user_id", -1);
    }

    private void saveCurrentUserId(int userId) {
        sharedPreferences.edit().putInt("current_user_id", userId).apply();
    }

    public LiveData<User> getCurrentUser() {
        int userId = getCurrentUserId();
        if (userId != -1) {
            return userDao.getUserById(userId);
        }
        return null;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // Fallback (not recommended for production)
        }
    }

    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    public interface RepositoryCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}