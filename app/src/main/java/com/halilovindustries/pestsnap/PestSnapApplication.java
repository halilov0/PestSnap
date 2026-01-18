package com.halilovindustries.pestsnap;

import android.app.Application;
import android.os.StrictMode;
import com.halilovindustries.pestsnap.data.remote.MockApiService;

public class PestSnapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Allow network on main thread for mock server initialization only
        // Remove this in production!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll()
                .build();
        StrictMode.setThreadPolicy(policy);

        // Start mock server early
        MockApiService.startMockServer();
    }
}
