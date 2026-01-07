package com.easydine.app;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReserveActivity extends AppCompatActivity {

    private static final String TAG = "ReserveActivity";
    private String restaurantId;
    private String selectedDateId = null;
    private Spinner spTime;
    private TextView tvSelectedDate;
    private NumberPicker npPartySize;

    private final List<String> availableTimes = new ArrayList<>();
    private ArrayAdapter<String> timeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve);

        restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId == null) {
            toast("Error: Restaurant ID is missing.");
            finish();
            return;
        }

        Button btnPickDate = findViewById(R.id.btnPickDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        spTime = findViewById(R.id.spTime);
        npPartySize = findViewById(R.id.npPartySize);
        Button btnBook = findViewById(R.id.btnBook);

        npPartySize.setMinValue(1);
        npPartySize.setMaxValue(10);
        npPartySize.setValue(2);

        timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableTimes);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTime.setAdapter(timeAdapter);

        btnPickDate.setOnClickListener(v -> pickDate());
        btnBook.setOnClickListener(v -> bookNow());
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        @SuppressLint("SetTextI18n") DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth);

            selectedDateId = formatDate(chosen);

            tvSelectedDate.setText("Selected date: " + selectedDateId);
            loadAvailabilityForDate(selectedDateId);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dlg.show();
    }

    /**
     * Loads availability for a given date from Firestore.
     * Expects a document at /restaurants/{restaurantId}/availability/{dateId}
     * with a "slots" map, where keys are times (e.g., "18:00") and values are remaining seats.
     */
    @SuppressWarnings("unchecked")
    private void loadAvailabilityForDate(String dateId) {
        DocumentReference availabilityDocRef = FirebaseFirestore.getInstance()
                .collection("restaurants")
                .document(restaurantId)
                .collection("availability")
                .document(dateId);

        Log.d(TAG, "Querying Firestore path: " + availabilityDocRef.getPath());

        availabilityDocRef.get().addOnSuccessListener(doc -> {
            Log.d(TAG, "Successfully fetched document. Exists: " + doc.exists());
            availableTimes.clear();

            if (!doc.exists()) {
                toast("No availability for this date");
                timeAdapter.notifyDataSetChanged();
                return;
            }

            Map<String, Object> slots = (Map<String, Object>) doc.get("slots");
            if (slots == null) {
                Log.d(TAG, "Document exists, but 'slots' field is null or missing.");
                toast("No availability for this date");
                timeAdapter.notifyDataSetChanged();
                return;
            }

            // Show only times with remaining > 0
            for (String timeKey : slots.keySet()) {
                Object remainingSlots = slots.get(timeKey);
                long remaining = (remainingSlots instanceof Number) ? ((Number) remainingSlots).longValue() : 0;
                if (remaining > 0) {
                    availableTimes.add(timeKey);
                }
            }

            Collections.sort(availableTimes);
            timeAdapter.notifyDataSetChanged();

            if (availableTimes.isEmpty()) {
                Log.d(TAG, "Slots field was found, but no times have remaining > 0.");
                toast("No available times for this date");
            } else {
                spTime.setSelection(0);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching availability from Firestore", e);
            toast("Error fetching availability: " + e.getMessage());
        });
    }

    private void bookNow() {
        if (selectedDateId == null) {
            toast("Pick a date first");
            return;
        }
        if (availableTimes.isEmpty()) {
            toast("No available times");
            return;
        }

        String selectedTime = (String) spTime.getSelectedItem();
        int partySize = npPartySize.getValue();

        FirebaseAuth auth = FirebaseAuth.getInstance();

        // For this demo, if the user isn't logged in with a real account
        // (e.g., from a partner integration), we sign them in anonymously
        // to ensure they have a UID for creating bookings.
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnSuccessListener(r -> doBookingTransaction(selectedTime, partySize))
                    .addOnFailureListener(e -> toast("Anonymous auth failed: " + e.getMessage()));
        } else {
            doBookingTransaction(selectedTime, partySize);
        }
    }

    /**
     * Executes the booking as a Firestore transaction to ensure atomicity.
     * 1. Fetches the availability document.
     * 2. Checks if there are enough seats.
     * 3. Decrements the seat count for the selected time slot.
     * 4. Creates a new booking document.
     */
    @SuppressWarnings("unchecked")
    private void doBookingTransaction(String selectedTime, int partySize) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        DocumentReference availRef = db.collection("restaurants")
                .document(restaurantId)
                .collection("availability")
                .document(selectedDateId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(availRef);
            if (!snap.exists()) throw new RuntimeException("NO_AVAIL_DOC");

            Map<String, Object> slots = (Map<String, Object>) snap.get("slots");
            if (slots == null) slots = new HashMap<>();

            Object remainingObj = slots.get(selectedTime);
            long remaining = (remainingObj instanceof Number) ? ((Number) remainingObj).longValue() : 0;

            if (remaining < partySize) throw new RuntimeException("NOT_ENOUGH_SEATS");

            // Update seats
            Map<String, Object> newSlots = new HashMap<>(slots);
            newSlots.put(selectedTime, remaining - partySize);
            transaction.update(availRef, "slots", newSlots);

            // Create booking
            DocumentReference bookingRef = db.collection("bookings").document();
            Map<String, Object> booking = new HashMap<>();
            booking.put("restaurantId", restaurantId);
            booking.put("userId", userId);
            booking.put("date", selectedDateId);
            booking.put("time", selectedTime);
            booking.put("partySize", partySize);
            booking.put("createdAt", FieldValue.serverTimestamp());

            transaction.set(bookingRef, booking);

            return bookingRef.getId();
        }).addOnSuccessListener(bookingId -> {
            toast("Booked! ID: " + bookingId);
            finish();
        }).addOnFailureListener(e -> {
            String msg = e.getMessage() == null ? "Booking failed" : e.getMessage();
            if (msg.contains("NOT_ENOUGH_SEATS")) msg = "Slot is full. Pick another time.";
            toast(msg);
        });
    }
    
    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private String formatDate(Calendar c) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return sdf.format(c.getTime());
    }
}
