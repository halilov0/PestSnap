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
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

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

        // Hide back button in fragment (we're using bottom nav)
        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }
    }

    private void observeTraps() {
        // Observe "Ready to Upload" and save to local list
        trapViewModel.getReadyToUploadTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            this.currentReadyTraps = traps != null ? traps : new ArrayList<>();
            updateSection(readyToUploadContainer, traps, "ready");
        });

        trapViewModel.getUploadingTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            updateSection(uploadingContainer, traps, "uploading");
        });

        trapViewModel.getQueuedTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            updateSection(queuedContainer, traps, "queued");
        });
    }

    private void updateSection(LinearLayout container, List<Trap> traps, String type) {
        container.removeAllViews();

        if (traps == null || traps.isEmpty()) {
            Log.d("QueueDebug", "No traps found for section: " + type);
            return;
        }

        for (Trap trap : traps) {
            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_upload_queue, container, false);

            TextView titleText = itemView.findViewById(R.id.uploadTitle);
            TextView statusText = itemView.findViewById(R.id.uploadStatus);
            ImageView thumbnailView = itemView.findViewById(R.id.uploadThumbnail);

            if (titleText != null) titleText.setText(trap.getTitle());

            String imagePath = trap.getImagePath();
            Log.d("QueueDebug", "Checking Trap: " + trap.getTitle());
            Log.d("QueueDebug", "Path from DB: " + imagePath);

            if (thumbnailView != null && imagePath != null) {
                File imgFile = new File(imagePath);
                boolean exists = imgFile.exists();
                Log.d("QueueDebug", "File exists? " + exists);

                if (exists) {
                    Log.d("QueueDebug", "File size: " + imgFile.length() + " bytes");
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = 8;

                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                        if (bitmap != null) {
                            Log.d("QueueDebug", "Bitmap created! Width: " + bitmap.getWidth());
                            thumbnailView.setImageBitmap(bitmap);
                            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } else {
                            Log.e("QueueDebug", "Bitmap is NULL");
                        }
                    } catch (Exception e) {
                        Log.e("QueueDebug", "EXCEPTION: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Log.e("QueueDebug", "File does NOT exist!");
                }
            }

            if (statusText != null) {
                String sizeStr = String.format(Locale.US, "%.1f MB", trap.getImageSize());
                if (type.equals("ready")) {
                    statusText.setText("Ready • " + sizeStr);
                } else if (type.equals("uploading")) {
                    statusText.setText("Uploading...");
                } else {
                    statusText.setText("Uploaded • Waiting for analysis");
                }
            }

            Button itemUploadBtn = itemView.findViewById(R.id.btnUpload);
            if (itemUploadBtn != null) {
                if (type.equals("ready")) {
                    itemUploadBtn.setVisibility(View.VISIBLE);
                    itemUploadBtn.setOnClickListener(v -> {
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
            // Check if there are traps to upload
            if (currentReadyTraps == null || currentReadyTraps.isEmpty()) {
                return;
            }

            // Upload from the cached list, NOT creating a new observer
            for (Trap trap : currentReadyTraps) {
                trapViewModel.uploadTrap(trap, String.valueOf(currentUserId));
            }
        });
    }
}
