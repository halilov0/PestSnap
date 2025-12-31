package com.halilovindustries.pestsnap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class QueueActivity extends AppCompatActivity {

    private Button backButton, uploadAllButton;
    private LinearLayout readyToUploadContainer, uploadingContainer, queuedContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        initializeViews();
        loadQueueItems();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        uploadAllButton = findViewById(R.id.uploadAllButton);
        readyToUploadContainer = findViewById(R.id.readyToUploadContainer);
        uploadingContainer = findViewById(R.id.uploadingContainer);
        queuedContainer = findViewById(R.id.queuedContainer);
    }

    private void loadQueueItems() {
        // Add sample queue items
        addQueueItem(readyToUploadContainer, "Trap #6 - North Field",
                "GPS: Ready • Size: 4.2 MB", "ready", 0);

        addQueueItem(uploadingContainer, "Trap #5 - South Field",
                "Uploading...", "uploading", 65);

        addQueueItem(queuedContainer, "Trap #4 - East Field",
                "In queue • Est. 40 min", "queued", 0);
    }

    private void addQueueItem(LinearLayout container, String title, String status,
                              String type, int progress) {
        View itemView = getLayoutInflater().inflate(R.layout.item_upload_queue, container, false);

        // Find views and set data
        // Implementation similar to previous adapters

        container.addView(itemView);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        uploadAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(QueueActivity.this,
                        "Uploading all traps...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}