package com.halilovindustries.pestsnap;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrapAdapter extends RecyclerView.Adapter<TrapAdapter.TrapViewHolder> {

    private List<TrapItem> trapList;
    private Context context;

    public TrapAdapter(List<TrapItem> trapList, Context context) {
        this.trapList = trapList;
        this.context = context;
    }

    @NonNull
    @Override
    public TrapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trap, parent, false);
        return new TrapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrapViewHolder holder, int position) {
        TrapItem trap = trapList.get(position);
        holder.titleText.setText(trap.getTitle());
        holder.timestampText.setText(trap.getTimestamp());
        holder.statusBadge.setText(trap.getStatus());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ResultsActivity.class);
                intent.putExtra("trapTitle", trap.getTitle());
                intent.putExtra("trapTimestamp", trap.getTimestamp());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trapList.size();
    }

    static class TrapViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, timestampText, statusBadge;

        public TrapViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.trapTitle);
            timestampText = itemView.findViewById(R.id.trapTimestamp);
            statusBadge = itemView.findViewById(R.id.statusBadge);
        }
    }
}