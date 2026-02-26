package com.easydine.app.ui.location;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.ArrayList;
import java.util.List;

public class PlacesPredictionsAdapter extends RecyclerView.Adapter<PlacesPredictionsAdapter.VH> {

    public interface OnClick {
        void onClick(AutocompletePrediction prediction);
    }

    private final OnClick onClick;
    private final List<AutocompletePrediction> items = new ArrayList<>();

    public PlacesPredictionsAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<AutocompletePrediction> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_prediction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AutocompletePrediction p = items.get(position);
        holder.primary.setText(p.getPrimaryText(null));
        holder.secondary.setText(p.getSecondaryText(null));

        holder.itemView.setOnClickListener(v -> onClick.onClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView primary, secondary;
        VH(@NonNull View itemView) {
            super(itemView);
            primary = itemView.findViewById(R.id.tvPrimary);
            secondary = itemView.findViewById(R.id.tvSecondary);
        }
    }
}