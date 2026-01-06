package com.halilovindustries.pestsnap.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.ResultsAdapter;
import com.halilovindustries.pestsnap.data.model.PestResult;
import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResultsFragment extends Fragment {

    private TextView pageTitle;
    private Button backButton;
    private RecyclerView resultsRecyclerView;
    private ResultsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_results, container, false);

        initializeViews(view);
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
    }

    private void loadResults() {
        // Check if arguments were passed
        Bundle args = getArguments();
        String trapTitle = null;

        if (args != null) {
            trapTitle = args.getString("trapTitle");
        }

        if (trapTitle != null) {
            pageTitle.setText("Result Details");
        } else {
            pageTitle.setText("Results");
        }

        // Create sample data
        List<TrapWithResults> results = createSampleResults();

        // Set adapter
        adapter = new ResultsAdapter(results);
        resultsRecyclerView.setAdapter(adapter);
    }

    private List<TrapWithResults> createSampleResults() {
        List<TrapWithResults> trapResults = new ArrayList<>();

        // Trap 1 - Single pest detected
        Trap trap1 = new Trap(1, "Trap #1 - South Field", "/path/image1.jpg",
                31.2612, 34.7991, 4.2f, 95, true);
        trap1.setId(1);
        trap1.setStatus("analyzed");
        trap1.setCapturedAt(System.currentTimeMillis() - 86400000); // Yesterday

        PestResult pest1 = new PestResult(1, "Thrips", "Frankliniella",
                8, 0.79f, null, false);
        pest1.setAnalyzedAt(trap1.getCapturedAt());

        trapResults.add(new TrapWithResults(trap1, Arrays.asList(pest1)));

        // Trap 2 - High infestation with warning
        Trap trap2 = new Trap(1, "Trap #2 - West Greenhouse", "/path/image2.jpg",
                31.2612, 34.7991, 3.8f, 92, true);
        trap2.setId(2);
        trap2.setStatus("analyzed");
        trap2.setCapturedAt(System.currentTimeMillis() - 90000000);

        PestResult pest2 = new PestResult(2, "Leafminer", "Liriomyza",
                42, 0.96f, "High infestation detected. Immediate intervention recommended.", true);
        pest2.setAnalyzedAt(trap2.getCapturedAt());

        trapResults.add(new TrapWithResults(trap2, Arrays.asList(pest2)));

        // Trap 3 - No pests detected
        Trap trap3 = new Trap(1, "Trap #3 - Center Field", "/path/image3.jpg",
                31.2612, 34.7991, 4.0f, 98, true);
        trap3.setId(3);
        trap3.setStatus("analyzed");
        trap3.setCapturedAt(System.currentTimeMillis());

        trapResults.add(new TrapWithResults(trap3, new ArrayList<>()));

        return trapResults;
    }

    // Method to update results from outside (optional)
    public void updateResults(List<TrapWithResults> newResults) {
        if (adapter != null) {
            adapter.updateResults(newResults);
        }
    }
}
