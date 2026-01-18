package com.halilovindustries.pestsnap.data.remote;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final boolean USE_MOCK = true;
    private static final String REAL_BASE_URL = "https://stardbi.cs.bgu.ac.il/";
    private static Retrofit retrofit;

    public static STARdbiApi getApiService() {
        Log.e(TAG, "===== getApiService() CALLED =====");
        return getRetrofitInstance().create(STARdbiApi.class);
    }

    private static Retrofit getRetrofitInstance() {
        Log.e(TAG, "===== getRetrofitInstance() CALLED =====");

        if (retrofit == null) {
            Log.e(TAG, "Retrofit is NULL - creating new instance");
            Log.e(TAG, "USE_MOCK = " + USE_MOCK);

            String baseUrl;
            if (USE_MOCK) {
                Log.e(TAG, "Starting MockWebServer...");
                baseUrl = MockApiService.startMockServer();
                Log.e(TAG, "MockWebServer URL: " + baseUrl);
            } else {
                baseUrl = REAL_BASE_URL;
            }

            if (baseUrl == null) {
                Log.e(TAG, "❌ BASE URL IS NULL!");
                throw new RuntimeException("Failed to get base URL");
            }

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = USE_MOCK ?
                    getSafeOkHttpClient(loggingInterceptor) :
                    getUnsafeOkHttpClient(loggingInterceptor);

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.e(TAG, "✅ Retrofit instance created successfully");
        } else {
            Log.e(TAG, "Retrofit already exists - reusing instance");
        }

        return retrofit;
    }

    private static OkHttpClient getSafeOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private static OkHttpClient getUnsafeOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        // ... keep your existing implementation
        return getSafeOkHttpClient(loggingInterceptor); // placeholder
    }
}
