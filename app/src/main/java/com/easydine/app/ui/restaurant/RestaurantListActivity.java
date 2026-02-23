package com.easydine.app.ui.restaurant;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;

import com.easydine.app.R;
import com.easydine.app.data.model.Restaurant;
import com.easydine.app.utils.LocationHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import com.easydine.app.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

        /*
        // 1. Initialize our helper
        LocationHelper locationHelper = new LocationHelper(this);

        // 2. Get Location and Sort
        locationHelper.getCurrentLocation(userLocation -> {
            if (userLocation != null) {

                // Loop through your list of restaurants
                for (Restaurant restaurant : restaurants) {

                    // Create a temporary Location object for the restaurant
                    Location restaurantLocation = new Location("");
                    restaurantLocation.setLatitude(restaurant.getLatitude());
                    restaurantLocation.setLongitude(restaurant.getLongitude());

                    // MAGIC: Calculate distance in meters!
                    float distanceInMeters = userLocation.distanceTo(restaurantLocation);

                    // Save it to the object (convert to KM if you prefer: distanceInMeters / 1000f)
                    restaurant.setDistanceToUser(distanceInMeters);
                }

                // 3. SORT THE LIST (Closest to Farthest)
                Collections.sort(restaurants, new Comparator<Restaurant>() {
                    @Override
                    public int compare(Restaurant r1, Restaurant r2) {
                        // This compares the distances and orders them smallest to largest
                        return Float.compare(r1.getDistanceToUser(), r2.getDistanceToUser());
                    }
                });

                // 4. UPDATE THE UI
                // Tell your RecyclerView adapter that the data order has changed!
                // myAdapter.notifyDataSetChanged();

                System.out.println("Restaurants sorted by distance!");

            } else {
                System.out.println("Could not get location. Loading default order.");
            }
        });

         */

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

                    // ONCE RESTAURANTS ARE LOADED, GET LOCATION AND SORT
                    LocationHelper locationHelper = new LocationHelper(RestaurantListActivity.this);
                    locationHelper.getCurrentLocation(userLocation -> {
                        if (userLocation != null) {
                            for (Restaurant restaurant : restaurants) {
                                Location restaurantLocation = new Location("");
                                restaurantLocation.setLatitude(restaurant.getLatitude());
                                restaurantLocation.setLongitude(restaurant.getLongitude());

                                float distanceInMeters = userLocation.distanceTo(restaurantLocation);
                                restaurant.setDistanceToUser(distanceInMeters);
                            }

                            // Sort the list
                            Collections.sort(restaurants, new Comparator<Restaurant>() {
                                @Override
                                public int compare(Restaurant r1, Restaurant r2) {
                                    return Float.compare(r1.getDistanceToUser(), r2.getDistanceToUser());
                                }
                            });
                        }

                        // Tell the adapter to update the screen!
                        adapter.notifyDataSetChanged();
                    });
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

        Restaurant r = new Restaurant(id, name, address, description);

        // Fetch coordinates from your GeoPoint field named "numAddress"
        com.google.firebase.firestore.GeoPoint geoPoint = doc.getGeoPoint("numAddress");

        if (geoPoint != null) {
            // GeoPoint automatically splits the latitude and longitude for us!
            r.setLatitude(geoPoint.getLatitude());
            r.setLongitude(geoPoint.getLongitude());
        }

        return r;
    }
}
