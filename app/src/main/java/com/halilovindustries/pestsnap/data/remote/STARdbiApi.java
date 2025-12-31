package com.halilovindustries.pestsnap.data.remote;

import com.halilovindustries.pestsnap.data.remote.model.AnalysisResponse;
import com.halilovindustries.pestsnap.data.remote.model.UploadRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface STARdbiApi {

    @POST("api/traps/upload")
    Call<AnalysisResponse> uploadTrap(@Body UploadRequest request);

    @GET("api/traps/{trapId}/status")
    Call<AnalysisResponse> getTrapStatus(@Path("trapId") String trapId);

    @GET("api/traps/{trapId}/results")
    Call<AnalysisResponse> getTrapResults(@Path("trapId") String trapId);
}