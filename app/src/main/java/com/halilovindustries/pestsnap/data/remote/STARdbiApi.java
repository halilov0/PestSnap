package com.halilovindustries.pestsnap.data.remote;

import com.halilovindustries.pestsnap.data.remote.model.AnalysisResponse;
import com.halilovindustries.pestsnap.data.remote.model.UploadResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface STARdbiApi {

    // 1. העלאת תמונה - שימוש ב-Multipart לפי ה-PDF
    @Multipart
    @POST("pestsnap/upload/")
    Call<UploadResponse> uploadTrap(
            @Part MultipartBody.Part image,        // התמונה עצמה
            @Part("GPS data") RequestBody gps,     // שים לב לרווח בשם - קריטי לשרת!
            @Part("Datetime stamp") RequestBody ts,// שים לב לרווח
            @Part("User ID") RequestBody uid       // שים לב לרווח
    );

    // 2. בדיקת סטטוס - מקבלים ID ומחזירים String (או JSON אם השרת תומך)
    // כרגע ה-PDF אומר שהתשובה היא טקסט ("In queue"), אז נשתמש ב-String
    // אם בעתיד זה יחזיר JSON מלא, נחליף ל-AnalysisResponse
    @GET("pestsnap/status/{trapId}/")
    Call<String> getTrapStatus(@Path("trapId") int trapId);

    // 3. קבלת תוצאות (אופציונלי - אם יש endpoint נפרד לתוצאות)
    @GET("api/traps/{trapId}/results")
    Call<AnalysisResponse> getTrapResults(@Path("trapId") String trapId);
}