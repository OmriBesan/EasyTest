package com.app.easydine.ui.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easydine.app.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class RestaurantDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantDetails";
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        restaurantId = getIntent().getStringExtra("restaurantId");

        TextView tvName = findViewById(R.id.tvName);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvDescription = findViewById(R.id.tvDescription);
        Button btnReserve = findViewById(R.id.btnReserve);

        // Show loading text until data is fetched
        tvName.setText("Loading...");
        tvAddress.setText("");
        tvDescription.setText("");

        FirebaseFirestore.getInstance()
                .collection("restaurants")
                .document(restaurantId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Restaurant not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    tvName.setText(Objects.toString(doc.getString("name"), "N/A"));
                    tvAddress.setText(Objects.toString(doc.getString("address"), "Address not available"));
                    tvDescription.setText(Objects.toString(doc.getString("description"), ""));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading restaurant details", e);
                    Toast.makeText(this, "Error loading details", Toast.LENGTH_LONG).show();
                    finish();
                });

        btnReserve.setOnClickListener(v -> {
            Intent i = new Intent(this, ReserveActivity.class);
            i.putExtra("restaurantId", restaurantId);
            startActivity(i);
        });
    }
}
