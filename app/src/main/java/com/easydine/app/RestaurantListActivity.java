package com.easydine.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RestaurantListActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantListActivity";
    private final List<Restaurant> restaurants = new ArrayList<>();
    private RestaurantAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        RecyclerView rv = findViewById(R.id.rvRestaurants);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RestaurantAdapter(restaurants, r -> {
            Intent i = new Intent(this, RestaurantDetailsActivity.class);
            i.putExtra("restaurantId", r.id);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        loadRestaurants();
    }

    private void loadRestaurants() {
        FirebaseFirestore.getInstance()
                .collection("restaurants")
                .get()
                .addOnSuccessListener(qs -> {
                    restaurants.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        restaurants.add(fromDoc(doc));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading restaurants", e);
                    Toast.makeText(this, "Error loading restaurants", Toast.LENGTH_SHORT).show();
                });
    }

    private Restaurant fromDoc(DocumentSnapshot doc) {
        String id = doc.getId();
        String name = doc.getString("name");
        String address = doc.getString("address");
        String description = doc.getString("description");
        return new Restaurant(id, name, address, description);
    }
}
