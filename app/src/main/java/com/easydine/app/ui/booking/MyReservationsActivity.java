package com.easydine.app.ui.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.data.model.Booking;
import com.easydine.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyReservationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReservationsAdapter adapter;
    private List<Booking> bookingList;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        recyclerView = findViewById(R.id.reservationsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();

        // Initialize Adapter with the "Cancel" click listener
        adapter = new ReservationsAdapter(bookingList, this::   cancelBooking);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadReservations();
    }

    private void loadReservations() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // Query: Get all bookings where userId matches the current user
        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    bookingList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            // Convert Firestore data to our Java Object
                            Booking booking = doc.toObject(Booking.class);

                            // --- THE FIX FOR OLD DATA ---
                            // Old bookings might not have the ID inside the fields.
                            // We manually grab the Document ID and put it in the object.
                            if (booking != null) {
                                booking.setBookingId(doc.getId());
                                bookingList.add(booking);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading bookings", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelBooking(Booking booking) {
        // This is where we handle the red "Cancel" button
        if (booking.getBookingId() == null) {
            Toast.makeText(this, "Error: Cannot cancel this booking", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("bookings").document(booking.getBookingId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Reservation Cancelled", Toast.LENGTH_SHORT).show();
                    // Remove from list and refresh
                    bookingList.remove(booking);
                    adapter.notifyDataSetChanged();

                    // Show empty view if list is now empty
                    if (bookingList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel", Toast.LENGTH_SHORT).show());
    }
}