package com.halilovindustries.pestsnap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.data.model.PestResult;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultViewHolder> {

    private List<TrapWithResults> trapResults;
    private SimpleDateFormat dateFormat;

    public ResultsAdapter(List<TrapWithResults> trapResults) {
        this.trapResults = trapResults;
        this.dateFormat = new SimpleDateFormat("'Yesterday,' hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result_detailed, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        TrapWithResults trapWithResults = trapResults.get(position);
        holder.bind(trapWithResults);
    }

    @Override
    public int getItemCount() {
        return trapResults.size();
    }

    class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView resultTitle;
        TextView resultTimestamp;
        TextView statusBadge;
        TextView detectedSpeciesLabel;
        LinearLayout pestContainer;
        LinearLayout pestSection;
        LinearLayout noPestSection;
        LinearLayout recommendationSection;
        TextView recommendationText;
        CardView mainCard;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            resultTitle = itemView.findViewById(R.id.resultTitle);
            resultTimestamp = itemView.findViewById(R.id.resultTimestamp);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            detectedSpeciesLabel = itemView.findViewById(R.id.detectedSpeciesLabel);
            pestContainer = itemView.findViewById(R.id.pestContainer);
            pestSection = itemView.findViewById(R.id.pestSection);
            noPestSection = itemView.findViewById(R.id.noPestSection);
            recommendationSection = itemView.findViewById(R.id.recommendationSection);
            recommendationText = itemView.findViewById(R.id.recommendationText);
            mainCard = itemView.findViewById(R.id.mainResultCard);
        }

        public void bind(TrapWithResults trapWithResults) {
            // Set title
            resultTitle.setText(trapWithResults.trap.getTitle());

            // Format and set timestamp
            String formattedDate = dateFormat.format(new Date(trapWithResults.trap.getCapturedAt()));
            resultTimestamp.setText("Analyzed: " + formattedDate);

            // Clear previous pest items
            pestContainer.removeAllViews();

            // Check if we have pest detections
            if (trapWithResults.results != null && !trapWithResults.results.isEmpty()) {
                pestSection.setVisibility(View.VISIBLE);
                noPestSection.setVisibility(View.GONE);
                detectedSpeciesLabel.setVisibility(View.VISIBLE);

                // Add each detected pest
                boolean hasWarning = false;
                String recommendation = null;

                for (PestResult result : trapWithResults.results) {
                    addPestItem(result);
                    if (result.isWarning()) {
                        hasWarning = true;
                    }
                    if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
                        recommendation = result.getRecommendation();
                    }
                }

                // Set status badge based on warnings
                if (hasWarning) {
                    setStatusBadge("⚠ Warning", R.color.warning, R.drawable.badge_warning);
                } else {
                    setStatusBadge("✓ Complete", R.color.success, R.drawable.badge_success);
                }

                // Show recommendations if available
                if (recommendation != null) {
                    recommendationSection.setVisibility(View.VISIBLE);
                    recommendationText.setText(recommendation);
                } else {
                    recommendationSection.setVisibility(View.GONE);
                }

            } else {
                // No pests detected
                pestSection.setVisibility(View.GONE);
                noPestSection.setVisibility(View.VISIBLE);
                setStatusBadge("Healthy Field", android.R.color.white, R.drawable.badge_healthy);

                // Show healthy field recommendation
                recommendationSection.setVisibility(View.VISIBLE);
                recommendationText.setText("No action required. Continue routine monitoring.");
            }
        }

        private void setStatusBadge(String text, int textColorRes, int backgroundRes) {
            statusBadge.setText(text);
            statusBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), textColorRes));
            statusBadge.setBackgroundResource(backgroundRes);
        }

        private void addPestItem(PestResult pestResult) {
            View pestView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_pest_info, pestContainer, false);

            TextView pestName = pestView.findViewById(R.id.pestName);
            TextView pestCount = pestView.findViewById(R.id.pestCount);

            // Format pest name
            String displayName = pestResult.getPestName();
            if (pestResult.getScientificName() != null && !pestResult.getScientificName().isEmpty()) {
                displayName += " (" + pestResult.getScientificName() + ")";
            }
            pestName.setText(displayName);

            // Format count and confidence
            int confidencePercent = Math.round(pestResult.getConfidence() * 100);
            pestCount.setText("Count: " + pestResult.getCount() + " • Confidence: " + confidencePercent + "%");

            pestContainer.addView(pestView);
        }
    }

    public void updateResults(List<TrapWithResults> newResults) {
        this.trapResults = newResults;
        notifyDataSetChanged();
    }
}