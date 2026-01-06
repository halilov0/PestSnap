package com.halilovindustries.pestsnap.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.halilovindustries.pestsnap.R;
import com.halilovindustries.pestsnap.TrapAdapter;
import com.halilovindustries.pestsnap.TrapItem;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerRecentTraps;
    private TrapAdapter trapAdapter;
    private TextView txtQueueCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Views
        recyclerRecentTraps = view.findViewById(R.id.recyclerRecentTraps);
        txtQueueCount = view.findViewById(R.id.txtQueueCount);

        setupRecyclerView();

        // Mock data for queue status
        txtQueueCount.setText("3 traps pending upload");

        return view;
    }

    private void setupRecyclerView() {
        // Reuse your existing Adapter logic
        List<TrapItem> trapList = new ArrayList<>();
        trapList.add(new TrapItem("Trap #1 - South Field", "Yesterday, 10:30 AM", "Complete", true));
        trapList.add(new TrapItem("Trap #2 - West Greenhouse", "Yesterday, 08:15 AM", "Complete", true));

        trapAdapter = new TrapAdapter(trapList, requireContext());
        recyclerRecentTraps.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecentTraps.setAdapter(trapAdapter);
    }
}