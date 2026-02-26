package com.easydine.app.ui.restaurant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.easydine.app.data.model.Restaurant;
import com.easydine.app.ui.booking.MyReservationsActivity;
import com.easydine.app.ui.location.LocationPermissionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantListActivity extends AppCompatActivity {

    private static final String PREFS = "easydine_prefs";

    private RecyclerView rvRestaurants;
    private RestaurantAdapter adapter;

    private final List<Restaurant> allRestaurants = new ArrayList<>();
    private final List<Restaurant> shownRestaurants = new ArrayList<>();

    private SharedPreferences sp;

    // Filters
    @Nullable private String selectedMood = null; // null => no mood filter
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        // If user never set location, send them to location flow
        if (!sp.getBoolean("location_set", false)) {
            startActivity(new Intent(this, LocationPermissionActivity.class));
            finish();
            return;
        }

        // UI
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        rvRestaurants = findViewById(R.id.rvRestaurants);

        // Show saved location name
        String locationName = sp.getString("location_name", "");
        if (locationName != null && !locationName.trim().isEmpty()) {
            tvLocation.setText(locationName);
        }

        // Recycler
        rvRestaurants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RestaurantAdapter(this,shownRestaurants, restaurant -> {
            Intent i = new Intent(this, RestaurantDetailsActivity.class);
            i.putExtra("restaurantId", restaurant.getId()); // keep your existing key
            startActivity(i);
            loadFavoritesIntoAdapter();
        });
        rvRestaurants.setAdapter(adapter);

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = (s == null) ? "" : s.toString().trim();
                applyFiltersAndRefresh();
            }
        });

        // Mood button opens sheet
        findViewById(R.id.ivMood).setOnClickListener(v -> showMoodSheet());

        // Show mood sheet once on first entry
        if (!sp.getBoolean("mood_chosen_once", false)) {
            showMoodSheet();
        } else {
            selectedMood = sp.getString("mood_value", null);
        }

        // Bottom nav
        setupBottomNav();

        // Load data
        loadRestaurantsFromFirestore();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_reserved) {
                startActivity(new Intent(this, MyReservationsActivity.class));
                return true;
            }

            if (id == R.id.nav_liked) {
                Toast.makeText(this, "Liked screen (TODO)", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (id == R.id.nav_account) {
                Toast.makeText(this, "Account screen (TODO)", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void showMoodSheet() {
        MoodBottomSheetDialogFragment sheet = new MoodBottomSheetDialogFragment();
        sheet.setListener(mood -> {
            // mood == null means "skip for now"
            selectedMood = mood;

            sp.edit()
                    .putBoolean("mood_chosen_once", true)
                    .putString("mood_value", mood)
                    .apply();

            applyFiltersAndRefresh();

            if (mood != null && !mood.trim().isEmpty()) {
                Toast.makeText(this, "Mood: " + mood, Toast.LENGTH_SHORT).show();
            }
        });
        sheet.show(getSupportFragmentManager(), "mood_sheet");
    }

    private void loadRestaurantsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("restaurants")
                .get()
                .addOnSuccessListener(qs -> {
                    allRestaurants.clear();

                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Restaurant r = fromDoc(doc);
                        if (r != null) allRestaurants.add(r);
                    }

                    // sort by distance if we have saved user location
                    Location userLoc = getSavedUserLocationOrNull();
                    if (userLoc != null) {
                        computeDistancesAndSort(userLoc, allRestaurants);
                    }

                    applyFiltersAndRefresh();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
                );
    }
    private void loadFavoritesIntoAdapter() {
        var user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("favorites")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener(qs -> {
                    java.util.Set<String> ids = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                        ids.add(d.getId()); // doc id = restaurantId
                    }
                    adapter.setFavoriteIds(ids);
                });
    }

    private void applyFiltersAndRefresh() {
        shownRestaurants.clear();

        for (Restaurant r : allRestaurants) {
            if (!passesMood(r)) continue;
            if (!passesSearch(r)) continue;
            shownRestaurants.add(r);
        }

        adapter.notifyDataSetChanged();
    }

    private boolean passesSearch(@NonNull Restaurant r) {
        if (searchQuery == null || searchQuery.isEmpty()) return true;

        String q = searchQuery.toLowerCase();
        String name = (r.getName() == null) ? "" : r.getName().toLowerCase();
        String addr = (r.getAddress() == null) ? "" : r.getAddress().toLowerCase();

        return name.contains(q) || addr.contains(q);
    }

    private boolean passesMood(@NonNull Restaurant r) {
        if (selectedMood == null || selectedMood.trim().isEmpty()) return true;

        List<String> moods = r.getMoods();
        if (moods == null) return false;

        // match exact string (recommended: keep values consistent between popup & Firestore)
        for (String m : moods) {
            if (m != null && m.equalsIgnoreCase(selectedMood)) return true;
        }
        return false;
    }

    @Nullable
    private Location getSavedUserLocationOrNull() {
        if (!sp.getBoolean("location_set", false)) return null;

        float lat = sp.getFloat("location_lat", 0f);
        float lng = sp.getFloat("location_lng", 0f);

        if (lat == 0f && lng == 0f) return null;

        Location user = new Location("saved");
        user.setLatitude(lat);
        user.setLongitude(lng);
        return user;
    }

    private void computeDistancesAndSort(@NonNull Location user, @NonNull List<Restaurant> list) {
        for (Restaurant r : list) {
            double rLat = r.getLatitude();
            double rLng = r.getLongitude();

            if (rLat == 0.0 && rLng == 0.0) {
                r.setDistanceToUser(Float.MAX_VALUE);
                continue;
            }

            Location rl = new Location("restaurant");
            rl.setLatitude(rLat);
            rl.setLongitude(rLng);

            r.setDistanceToUser(user.distanceTo(rl));
        }

        Collections.sort(list, (a, b) -> Float.compare(a.getDistanceToUser(), b.getDistanceToUser()));
    }

    @Nullable
    private Restaurant fromDoc(@NonNull DocumentSnapshot doc) {
        String id = doc.getId();

        String name = doc.getString("name");
        String address = doc.getString("address");
        String description = doc.getString("description");

        Restaurant r = new Restaurant(id, name, address, description);

        // GeoPoint -> lat/lng
        GeoPoint gp = doc.getGeoPoint("numAddress");
        if (gp != null) {
            r.setLatitude(gp.getLatitude());
            r.setLongitude(gp.getLongitude());
        }

        // moods array
        List<String> moods = (List<String>) doc.get("moods");
        r.setMoods(moods);

        // placeId (you said you'll store it)
        String placeId = doc.getString("placeId");
        r.setPlaceId(placeId);

        return r;
    }
}