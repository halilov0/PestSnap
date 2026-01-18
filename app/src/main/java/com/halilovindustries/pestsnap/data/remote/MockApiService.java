package com.halilovindustries.pestsnap.data.remote;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class MockApiService {

    private static final String TAG = "MockServer";
    private static MockWebServer mockServer;
    private static String mockBaseUrl = null;
    private static int nextId = 1;

    // 🆕 Track upload times: trapId -> upload timestamp
    private static Map<Integer, Long> uploadTimes = new HashMap<>();

    public static String startMockServer() {
        if (mockBaseUrl != null) {
            Log.d(TAG, "Server already running at: " + mockBaseUrl);
            return mockBaseUrl;
        }

        try {
            mockServer = new MockWebServer();
            mockServer.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    Log.d(TAG, "=== DISPATCHER CALLED ===");
                    Log.d(TAG, "Path: " + request.getPath());
                    Log.d(TAG, "Method: " + request.getMethod());

                    String path = request.getPath();
                    String method = request.getMethod();

                    // Match upload endpoint
                    if (path != null && path.contains("/pestsnap/upload") && "POST".equals(method)) {
                        Log.d(TAG, "Matched UPLOAD endpoint");
                        return handleUpload();
                    }

                    // Match status endpoint
                    if (path != null && path.matches(".*\\/pestsnap\\/status\\/\\d+\\/?") && "GET".equals(method)) {
                        Log.d(TAG, "Matched STATUS endpoint");
                        // Extract trapId from path
                        String[] parts = path.split("/");
                        int trapId = Integer.parseInt(parts[parts.length - 1].replace("/", ""));
                        return handleStatus(trapId);
                    }

                    Log.e(TAG, "NO MATCH - returning 404");
                    return new MockResponse()
                            .setResponseCode(404)
                            .setBody("Not Found: " + path);
                }
            });

            mockServer.start();
            mockBaseUrl = mockServer.url("/").toString();
            Log.d(TAG, "✅ MockWebServer started at: " + mockBaseUrl);
            return mockBaseUrl;

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to start MockWebServer", e);
            e.printStackTrace();
            return null;
        }
    }

    private static MockResponse handleUpload() {
        Log.d(TAG, "handleUpload() executing...");
        try {
            Thread.sleep(1000); // Simulate network delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int id = nextId++;

        // 🆕 Record upload time
        long uploadTime = System.currentTimeMillis();
        uploadTimes.put(id, uploadTime);
        Log.d(TAG, "📝 Stored upload time for ID " + id + ": " + uploadTime);

        String json = String.format("{\"id\": %d}", id);
        Log.d(TAG, "Returning JSON: " + json);

        return new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(json);
    }

    private static MockResponse handleStatus(int trapId) {
        Log.d(TAG, "handleStatus() executing for trapId: " + trapId);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Long uploadTime = uploadTimes.get(trapId);

        if (uploadTime == null) {
            Log.e(TAG, "❌ No upload time found for trapId: " + trapId);
            return new MockResponse()
                    .setResponseCode(404)
                    .setBody("Trap not found");
        }

        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - uploadTime) / 1000;

        Log.d(TAG, "⏱️ Time elapsed since upload: " + elapsedSeconds + " seconds");

        // 🆕 Wait 5 seconds before returning results
        if (elapsedSeconds < 5) {
            Log.d(TAG, "⏳ Still in queue (< 5 seconds)");
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/plain; charset=utf-8")
                    .setBody("In queue");
        }

        // 🆕 After 5 seconds, return random fake results
        Log.d(TAG, "✅ Analysis complete! Returning results");
        String[] results = {
                "{\"status\":\"analyzed\",\"detectedPests\":[{\"commonName\":\"Thrips\",\"scientificName\":\"Frankliniella\",\"count\":8,\"confidence\":0.79}],\"requiresAction\":false,\"recommendation\":\"Monitor\"}",
                "{\"status\":\"analyzed\",\"detectedPests\":[{\"commonName\":\"Leafminer\",\"scientificName\":\"Liriomyza\",\"count\":22,\"confidence\":0.96}],\"requiresAction\":true,\"recommendation\":\"Immediate treatment recommended\"}",
                "{\"status\":\"analyzed\",\"detectedPests\":[],\"requiresAction\":false,\"recommendation\":\"Healthy Field - No pests detected\"}",
                "{\"status\":\"analyzed\",\"detectedPests\":[{\"commonName\":\"Aphid\",\"scientificName\":\"Aphidoidea\",\"count\":45,\"confidence\":0.92},{\"commonName\":\"Whitefly\",\"scientificName\":\"Aleyrodidae\",\"count\":23,\"confidence\":0.88}],\"requiresAction\":true,\"recommendation\":\"High pest activity - Treatment required\"}"
        };

        Random random = new Random();
        String result = results[random.nextInt(results.length)];

        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(result);
    }

    public static void stopMockServer() {
        if (mockServer != null) {
            try {
                mockServer.shutdown();
                mockBaseUrl = null;
                uploadTimes.clear();
                Log.d(TAG, "MockWebServer stopped");
            } catch (IOException e) {
                Log.e(TAG, "Error stopping server", e);
                e.printStackTrace();
            }
        }
    }
}
