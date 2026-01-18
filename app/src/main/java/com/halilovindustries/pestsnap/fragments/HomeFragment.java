package com.halilovindustries.pestsnap.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.TrapAdapter;
import com.halilovindustries.pestsnap.TrapItem;
import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.model.TrapWithResults; // 🆕 חשוב!
import com.halilovindustries.pestsnap.data.repository.UserRepository;
import com.halilovindustries.pestsnap.viewmodel.TrapViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerRecentTraps;
    private TrapAdapter trapAdapter;
    private TextView txtQueueCount;

    private TrapViewModel trapViewModel;
    private UserRepository userRepository;
    private int currentUserId;

    // משתנים לשמירת ספירת הממתינים (הלוגיקה הזו נשארת זהה)
    private int readyCount = 0;
    private int uploadingCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        recyclerRecentTraps = view.findViewById(R.id.recyclerRecentTraps);
        txtQueueCount = view.findViewById(R.id.txtQueueCount);

        // Setup RecyclerView
        recyclerRecentTraps.setLayoutManager(new LinearLayoutManager(requireContext()));
        trapAdapter = new TrapAdapter(new ArrayList<>(), requireContext());
        recyclerRecentTraps.setAdapter(trapAdapter);

        // Initialize Data Layer
        userRepository = new UserRepository(requireContext());
        currentUserId = userRepository.getCurrentUserId();
        if (currentUserId == -1) currentUserId = 1;

        trapViewModel = new ViewModelProvider(this).get(TrapViewModel.class);

        // Start Observing Data
        observeRecentResultsOnly(); // שינינו את השם כדי שיהיה ברור
        observeQueueCounts();

        return view;
    }

    private void observeRecentResultsOnly() {
        // אנחנו מאזינים ל-TrapsWithResults כדי לוודא שיש לנו את כל המידע
        // ה-DAO ממיין לפי capturedAt DESC, אז החדשים ביותר ראשונים
        trapViewModel.getAllTrapsWithResults(currentUserId).observe(getViewLifecycleOwner(), allTraps -> {
            if (allTraps == null) return;

            List<Trap> filteredList = new ArrayList<>();

            // 🔥 הסינון החכם: רק מה שמוכן!
            for (TrapWithResults item : allTraps) {
                // התנאי: הסטטוס חייב להיות analyzed
                if ("analyzed".equals(item.trap.getStatus())) {
                    filteredList.add(item.trap);
                }

                // אנחנו רוצים רק את ה-5 האחרונים שעומדים בתנאי
                if (filteredList.size() >= 5) {
                    break; // מפסיקים את הלולאה ברגע שהשגנו 5
                }
            }

            // אם הרשימה ריקה (אין עדיין תוצאות), אפשר להציג משהו אחר או להשאיר ריק
            if (filteredList.isEmpty()) {
                // אופציונלי: כאן אפשר להסתיר את ה-RecyclerView אם רוצים
            }

            // המרה לתצוגה
            List<TrapItem> uiItems = convertTrapsToItems(filteredList);

            // עדכון המסך
            trapAdapter = new TrapAdapter(uiItems, requireContext());
            recyclerRecentTraps.setAdapter(trapAdapter);
        });
    }

    private void observeQueueCounts() {
        // זה נשאר אותו דבר - מציג כמה ממתינים להעלאה
        trapViewModel.getReadyToUploadTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            readyCount = (traps != null) ? traps.size() : 0;
            updateQueueText();
        });

        trapViewModel.getUploadingTraps(currentUserId).observe(getViewLifecycleOwner(), traps -> {
            uploadingCount = (traps != null) ? traps.size() : 0;
            updateQueueText();
        });
    }

    private void updateQueueText() {
        int totalPending = readyCount + uploadingCount;
        if (totalPending == 0) {
            txtQueueCount.setText("All caught up! No pending uploads.");
        } else {
            txtQueueCount.setText(totalPending + " traps pending upload");
        }
    }

    private List<TrapItem> convertTrapsToItems(List<Trap> dbTraps) {
        List<TrapItem> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

        for (Trap trap : dbTraps) {
            String formattedDate = sdf.format(new Date(trap.getCapturedAt()));

            // כאן אנחנו יודעים בוודאות שהכל "Complete" כי סיננו למעלה
            // אבל ליתר ביטחון נשאיר את הלוגיקה הגנרית
            String displayStatus = "Analyzed";
            boolean isCompleted = true;

            items.add(new TrapItem(
                    trap.getTitle(),
                    formattedDate,
                    displayStatus,
                    isCompleted
            ));
        }
        return items;
    }
}