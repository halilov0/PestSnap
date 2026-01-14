package com.halilovindustries.pestsnap.data.remote;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
// חשוב: אם אתה משתמש ב-getStatus שמחזיר String, צריך גם את זה:
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    // הכתובת הבסיסית (בלי pestsnap/upload בסוף)
    private static final String BASE_URL = "https://stardbi.cs.bgu.ac.il/";
    private static Retrofit retrofit;

    public static STARdbiApi getApiService() {
        return getRetrofitInstance().create(STARdbiApi.class);
    }

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // הוספת לוגים כדי לראות מה נשלח
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // שימוש בלקוח ה"לא בטוח" כדי לעקוף את שגיאת ה-SSL של האוניברסיטה
            OkHttpClient client = getUnsafeOkHttpClient(loggingInterceptor);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create()) // בשביל קבלת String בסטטוס
                    .addConverterFactory(GsonConverterFactory.create())    // בשביל קבלת JSON בהעלאה
                    .build();
        }
        return retrofit;
    }

    // --- הקסם שפותר את קריסות האבטחה ---
    private static OkHttpClient getUnsafeOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        try {
            // יצירת TrustManager שסומך על כולם
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // ביטול אימות Hostname
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}