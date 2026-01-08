package com.easydine.app.ui.restaurant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.easydine.app.data.model.Restaurant;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.VH> {

    /**
     * A callback interface to handle clicks on items in the list. This allows the adapter
     * to notify the hosting Activity or Fragment without needing a direct reference to it.
     */
    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant r);
    }

    private final List<Restaurant> items;
    private final OnRestaurantClickListener onClickListener;

    public RestaurantAdapter(List<Restaurant> items, OnRestaurantClickListener onClickListener) {
        this.items = items;
        this.onClickListener = onClickListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Restaurant r = items.get(position);
        holder.tvName.setText(r.name);
        holder.tvAddress.setText(r.address);
        holder.itemView.setOnClickListener(v -> onClickListener.onRestaurantClick(r));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
        }
    }
}
