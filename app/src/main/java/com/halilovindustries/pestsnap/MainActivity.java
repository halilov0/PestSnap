package com.halilovindustries.pestsnap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button captureTrapButton, uploadQueueButton, logoutButton;
    private RecyclerView recentTrapsRecyclerView;
    private TrapAdapter trapAdapter;
    private List<TrapItem> trapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeViews() {
        captureTrapButton = findViewById(R.id.captureTrapButton);
        uploadQueueButton = findViewById(R.id.uploadQueueButton);
        logoutButton = findViewById(R.id.logoutButton);
        recentTrapsRecyclerView = findViewById(R.id.recentTrapsRecyclerView);
    }

    private void setupRecyclerView() {
        trapList = new ArrayList<>();

        // Sample data
        trapList.add(new TrapItem("Trap #1 - South Field", "Yesterday, 10:30 AM", "Complete", true));
        trapList.add(new TrapItem("Trap #2 - West Greenhouse", "Yesterday, 08:15 AM", "Complete", true));
        trapList.add(new TrapItem("Trap #3 - Center Field", "Today, 11:00 AM", "Complete", true));

        trapAdapter = new TrapAdapter(trapList, this);
        recentTrapsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentTrapsRecyclerView.setAdapter(trapAdapter);
    }

    private void setupClickListeners() {
        captureTrapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        uploadQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QueueActivity.class);
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}