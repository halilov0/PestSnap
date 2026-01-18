package com.halilovindustries.pestsnap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "ResultsActivity";

    private TextView pageTitle;
    private Button backButton;
    private RecyclerView resultsRecyclerView;
    private ResultsAdapter adapter;

    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        initializeViews();

        // Initialize Data Layer
        userRepository = new UserRepository(this);
        currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == -1) currentUserId = 1;

        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);

        loadResults();
        setupClickListeners();
    }

    private void initializeViews() {
        pageTitle = findViewById(R.id.pageTitle);
        backButton = findViewById(R.id.backButton);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize with empty adapter
        adapter = new ResultsAdapter(new ArrayList<>());
        resultsRecyclerView.setAdapter(adapter);
    }

    private void loadResults() {
        pageTitle.setText("Results");

        // 🆕 Observe REAL database data for "analyzed" traps
        trapViewModel.getAllTrapsWithResults(currentUserId).observe(this, trapResults -> {
            Log.d(TAG, "📊 Results changed - Count: " + (trapResults != null ? trapResults.size() : 0));

            if (trapResults != null) {
                // Filter only "analyzed" traps
                List<TrapWithResults> analyzedTraps = new ArrayList<>();
                for (TrapWithResults tr : trapResults) {
                    if ("analyzed".equals(tr.trap.getStatus())) {
                        analyzedTraps.add(tr);
                        Log.d(TAG, "  → ANALYZED Trap: " + tr.trap.getTitle() + " with " + tr.results.size() + " pests");
                    }
                }

                adapter.updateResults(analyzedTraps);
            }
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }
}
