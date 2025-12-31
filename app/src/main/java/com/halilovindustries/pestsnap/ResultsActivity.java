package com.halilovindustries.pestsnap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ResultsActivity extends AppCompatActivity {

    private TextView pageTitle;
    private Button backButton;
    private LinearLayout resultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        initializeViews();
        loadResults();
        setupClickListeners();
    }

    private void initializeViews() {
        pageTitle = findViewById(R.id.pageTitle);
        backButton = findViewById(R.id.backButton);
        resultsContainer = findViewById(R.id.resultsContainer);
    }

    private void loadResults() {
        // Get data from intent if passed
        String trapTitle = getIntent().getStringExtra("trapTitle");

        if (trapTitle != null) {
            // Show specific result
            pageTitle.setText("Result Details");
        } else {
            // Show all results
            pageTitle.setText("Results");
        }

        // Add sample results dynamically
        addResultCard("Trap #1 - South Field", "Yesterday, 10:30 AM",
                "Thrips (Frankliniella)", "8", "79%", null, false);

        addResultCard("Trap #2 - West Greenhouse", "Yesterday, 08:15 AM",
                "Leafminer (Liriomyza)", "42", "96%",
                "High infestation detected. Immediate intervention recommended.", true);

        addResultCard("Trap #3 - Center Field", "Today, 11:00 AM",
                null, null, null,
                "The field appears clear of infestation.", false);
    }

    private void addResultCard(String title, String timestamp, String pestName,
                               String count, String confidence, String recommendation,
                               boolean isWarning) {
        View cardView = getLayoutInflater().inflate(R.layout.item_result_card, resultsContainer, false);

        TextView titleText = cardView.findViewById(R.id.resultTitle);
        TextView timestampText = cardView.findViewById(R.id.resultTimestamp);
        TextView pestNameText = cardView.findViewById(R.id.pestName);
        TextView pestCountText = cardView.findViewById(R.id.pestCount);
        LinearLayout pestSection = cardView.findViewById(R.id.pestSection);
        LinearLayout noPestSection = cardView.findViewById(R.id.noPestSection);
        TextView recommendationText = cardView.findViewById(R.id.recommendationText);
        LinearLayout recommendationSection = cardView.findViewById(R.id.recommendationSection);
        CardView mainCard = cardView.findViewById(R.id.mainResultCard);

        titleText.setText(title);
        timestampText.setText("Analyzed: " + timestamp);

        if (pestName != null) {
            pestSection.setVisibility(View.VISIBLE);
            noPestSection.setVisibility(View.GONE);
            pestNameText.setText(pestName);
            pestCountText.setText("Count: " + count + " • Confidence: " + confidence);
        } else {
            pestSection.setVisibility(View.GONE);
            noPestSection.setVisibility(View.VISIBLE);
        }

        if (recommendation != null) {
            recommendationSection.setVisibility(View.VISIBLE);
            recommendationText.setText(recommendation);

            if (isWarning) {
                mainCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
            }
        } else {
            recommendationSection.setVisibility(View.GONE);
        }

        resultsContainer.addView(cardView);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}