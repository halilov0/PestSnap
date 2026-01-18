package com.halilovindustries.pestsnap.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.ResultsAdapter;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

import java.util.ArrayList;
import java.util.List;

public class ResultsFragment extends Fragment {
    private static final String TAG = "ResultsFragment";

    private TextView pageTitle;
    private Button backButton;
    private RecyclerView resultsRecyclerView;
    private ResultsAdapter adapter;

    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_results, container, false);

        initializeViews(view);

        // Initialize Data Layer
        userRepository = new UserRepository(requireContext());
        currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == -1) currentUserId = 1;

        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);

        loadResults();

        return view;
    }

    private void initializeViews(View view) {
        pageTitle = view.findViewById(R.id.pageTitle);
        backButton = view.findViewById(R.id.backButton);
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);

        // Hide back button (using bottom nav instead)
        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize with empty adapter
        adapter = new ResultsAdapter(new ArrayList<>());
        resultsRecyclerView.setAdapter(adapter);
    }

    private void loadResults() {
        pageTitle.setText("Results");

        // 🆕 Observe REAL database data
        trapViewModel.getAllTrapsWithResults(currentUserId).observe(getViewLifecycleOwner(), trapResults -> {
            Log.d(TAG, "📊 Results changed - Count: " + (trapResults != null ? trapResults.size() : 0));

            if (trapResults != null) {
                // Filter only "analyzed" traps
                List<TrapWithResults> analyzedTraps = new ArrayList<>();
                for (TrapWithResults tr : trapResults) {
                    Log.d(TAG, "  → Trap: " + tr.trap.getTitle() + " Status: " + tr.trap.getStatus() + " Pests: " + tr.results.size());

                    if ("analyzed".equals(tr.trap.getStatus())) {
                        analyzedTraps.add(tr);
                        Log.d(TAG, "    ✅ ADDED to Results (analyzed)");
                    }
                }

                Log.d(TAG, "🎯 Total analyzed traps to display: " + analyzedTraps.size());
                adapter.updateResults(analyzedTraps);
            }
        });
    }

    // Method to update results from outside (optional)
    public void updateResults(List<TrapWithResults> newResults) {
        if (adapter != null) {
            adapter.updateResults(newResults);
        }
    }
}
