package com.easydine.app.ui.booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.easydine.app.data.model.Booking;

import java.util.List;

public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {

    private List<Booking> bookingList;
    private OnReservationAction listener;

    // Interface to send click events back to the Activity
    public interface OnReservationAction {
        void onCancelBooking(Booking booking);
    }

    // Constructor
    public ReservationsAdapter(List<Booking> bookingList, OnReservationAction listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // --- BIND DATA TO VIEWS ---
        // Note: If you only have restaurantId, we might need to fetch the name later.
        // For now, let's assume your Booking model has these fields.
        holder.tvRestaurantName.setText(booking.getRestaurantId());
        holder.tvDateTime.setText(booking.getDate() + " at " + booking.getTime());
        holder.tvPartySize.setText(booking.getPartySize() + " Guests");

        // Handle Cancel Button Click
        holder.btnCancel.setOnClickListener(v -> {
            listener.onCancelBooking(booking);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    // The ViewHolder class holds the UI elements for one row
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRestaurantName, tvDateTime, tvPartySize;
        Button btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.textRestaurantName);
            tvDateTime = itemView.findViewById(R.id.textDateTime);
            tvPartySize = itemView.findViewById(R.id.textPartySize);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}