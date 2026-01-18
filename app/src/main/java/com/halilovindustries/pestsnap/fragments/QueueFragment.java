package com.halilovindustries.pestsnap.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.utils.NetworkUtils;
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QueueFragment extends Fragment {

    private Button backButton, uploadAllButton;
    private LinearLayout readyToUploadContainer, uploadingContainer, queuedContainer;

    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    // Store the current ready traps to avoid observer loops
    private List<Trap> currentReadyTraps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_queue, container, false);

        initializeViews(view);

        // Initialize Data Layer
        userRepository = new UserRepository(requireContext());
        currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == -1) currentUserId = 1;

        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);

        // Start observing database changes
        observeTraps();

        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
        uploadAllButton = view.findViewById(R.id.uploadAllButton);
        readyToUploadContainer = view.findViewById(R.id.readyToUploadContainer);
        uploadingContainer = view.findViewById(R.id.uploadingContainer);
        queuedContainer = view.findViewById(R.id.queuedContainer);

        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }
    }

    // משתנה עזר לשמירת שמות המלכודות שכבר סיימו
    private List<String> finishedTrapTitles = new ArrayList<>();
    private List<Trap> currentQueuedTraps = new ArrayList<>();

    private void observeTraps() {
        trapViewModel.getAllTrapsWithResults(currentUserId).observe(getViewLifecycleOwner(), results -> {
            finishedTrapTitles.clear();
            if (results != null) {
                for (TrapWithResults tr : results) {
                    if (!tr.results.isEmpty() || "analyzed".equals(tr.trap.getStatus())) {
                        finishedTrapTitles.add(tr.trap.getTitle());
                    }
                }
            }
            if (currentQueuedTraps != null) {
                updateSection(queuedContainer, currentQueuedTraps, "queued");
            }
        });

        trapViewModel.getQueuedTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            this.currentQueuedTraps = traps;
            updateSection(queuedContainer, traps, "queued");
        });

        trapViewModel.getReadyToUploadTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            this.currentReadyTraps = traps;
            updateSection(readyToUploadContainer, traps, "ready");
        });

        trapViewModel.getUploadingTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            updateSection(uploadingContainer, traps, "uploading");
        });
    }

    private void updateSection(LinearLayout container, List<Trap> traps, String type) {
        container.removeAllViews();

        if (traps == null || traps.isEmpty()) {
            return;
        }

        for (Trap trap : traps) {
            String trapTitle = trap.getTitle();

            // סינון תוצאות כפולות (מהתיקון הקודם)
            if (type.equals("queued") && finishedTrapTitles.contains(trapTitle)) {
                continue;
            }

            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_upload_queue, container, false);

            TextView titleText = itemView.findViewById(R.id.uploadTitle);
            TextView statusText = itemView.findViewById(R.id.uploadStatus);
            ImageView thumbnailView = itemView.findViewById(R.id.uploadThumbnail);

            if (titleText != null) titleText.setText(trap.getTitle());

            String imagePath = trap.getImagePath();
            if (thumbnailView != null && imagePath != null) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 8;
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        if (bitmap != null) {
                            thumbnailView.setImageBitmap(bitmap);
                            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (statusText != null) {
                String sizeStr = String.format(Locale.US, "%.1f MB", trap.getImageSize());
                if (type.equals("ready")) {
                    statusText.setText("Ready • " + sizeStr);
                } else if (type.equals("uploading")) {
                    // כאן נוכל להציג הודעה חכמה יותר אם אין אינטרנט
                    if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        statusText.setText("Waiting for internet connection...");
                    } else {
                        statusText.setText("Uploading...");
                    }
                } else {
                    statusText.setText("Uploaded • Waiting for analysis");
                }
            }

            Button itemUploadBtn = itemView.findViewById(R.id.btnUpload);
            if (itemUploadBtn != null) {
                if (type.equals("ready")) {
                    itemUploadBtn.setVisibility(View.VISIBLE);
                    itemUploadBtn.setOnClickListener(v -> {
                        // 🟢 שינוי: לא חוסמים, רק מודיעים וממשיכים
                        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                            Toast.makeText(requireContext(),
                                    "No internet. Added to upload queue.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // הפונקציה הזו תשנה את הסטטוס ל-uploading מיד
                        trapViewModel.uploadTrap(trap, String.valueOf(currentUserId));
                    });
                } else {
                    itemUploadBtn.setVisibility(View.GONE);
                }
            }

            container.addView(itemView);
        }
    }

    private void setupClickListeners() {
        uploadAllButton.setOnClickListener(v -> {
            if (currentReadyTraps == null || currentReadyTraps.isEmpty()) {
                return;
            }

            // 🟢 שינוי: גם כאן, לא חוסמים, רק מודיעים
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(),
                        "No internet. All items added to upload queue.",
                        Toast.LENGTH_LONG).show();
            }

            for (Trap trap : currentReadyTraps) {
                trapViewModel.uploadTrap(trap, String.valueOf(currentUserId));
            }
        });
    }
}