package com.easydine.app.ui.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easydine.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageScheduleActivity extends AppCompatActivity {

    private Button btnSelectDate, btnSaveSchedule;
    private Spinner spinnerStart, spinnerEnd;
    private String restaurantId;

    // Data
    private String selectedDateId = null; // e.g., "20-01-2026"
    private final String[] times = {
            "10:00", "11:00", "12:00", "13:00", "14:00", "15:00",
            "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_schedule);

        // --- NEW: GRAB THE ID ---
        restaurantId = getIntent().getStringExtra("RESTAURANT_ID");

        if (restaurantId == null) {
            Toast.makeText(this, "Error: No Restaurant ID found!", Toast.LENGTH_LONG).show();
            finish(); // Close if we don't know who we are
            return;
        }
        // ------------------------

        // 1. Initialize Views
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);
        spinnerStart = findViewById(R.id.spinnerStart);
        spinnerEnd = findViewById(R.id.spinnerEnd);

        // 2. Setup Spinners (00:00 to 23:00)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, times);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStart.setAdapter(adapter);
        spinnerEnd.setAdapter(adapter);

        // Set Default: 12:00 to 22:00
        spinnerStart.setSelection(2); // 12:00
        spinnerEnd.setSelection(12);  // 22:00

        // 3. Listeners
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSaveSchedule.setOnClickListener(v -> saveScheduleToFirestore());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Format: DD-MM-YYYY
            selectedDateId = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year);
            btnSelectDate.setText("Date: " + selectedDateId);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void saveScheduleToFirestore() {
        if (selectedDateId == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        String start = spinnerStart.getSelectedItem().toString();
        String end = spinnerEnd.getSelectedItem().toString();

        // Basic Validation
        if (start.compareTo(end) >= 0) {
            Toast.makeText(this, "End time must be after Start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // GENERATE SLOTS
        // We will create a map where Key = "17:00" and Value = 20 (seats)
        Map<String, Integer> slotsMap = new HashMap<>();

        boolean isOpen = false;
        for (String t : times) {
            if (t.equals(start)) isOpen = true; // Start filling

            if (isOpen) {
                slotsMap.put(t, 20); // DEFAULT 20 SEATS PER HOUR
            }

            if (t.equals(end)) isOpen = false; // Stop filling
        }

        // SAVE TO FIRESTORE
        // Path: restaurants/{restaurantId}/availability/{dateId}
        // Note: For now, I'm hardcoding a restaurant ID or using User ID.
        // Ideally, the Owner's User ID IS the Restaurant ID.
       // String myRestaurantId = FirebaseAuth.getInstance().getUid();

        // Data Packet
        Map<String, Object> data = new HashMap<>();
        data.put("slots", slotsMap);

        FirebaseFirestore.getInstance()
                .collection("restaurants")
                .document(restaurantId) // Warning: Make sure this document exists!
                .collection("availability")
                .document(selectedDateId)
                .set(data)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Schedule Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}