package com.easydine.app.ui.location;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EnterLocationManuallyActivity extends AppCompatActivity {

    public static final String EXTRA_LOCATION_NAME = "extra_location_name";
    public static final String EXTRA_LOCATION_ADDRESS = "extra_location_address";
    public static final String EXTRA_LOCATION_LAT = "extra_location_lat";
    public static final String EXTRA_LOCATION_LNG = "extra_location_lng";

    private static final int REQ_LOCATION = 9001;

    private PlacesClient placesClient;
    private PlacesPredictionsAdapter adapter;

    private AutocompleteSessionToken sessionToken;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_location_manually);

        // Places init
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        // Location client (for "Use my current location")
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        // Toolbar back
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarEnterLocation);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Recycler
        RecyclerView rv = findViewById(R.id.rvSearchResults);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlacesPredictionsAdapter(this::onPredictionClicked);
        rv.setAdapter(adapter);

        // Search input
        com.google.android.material.textfield.TextInputEditText et = findViewById(R.id.etLocationSearch);

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Start a fresh session when user begins a new search
                sessionToken = AutocompleteSessionToken.newInstance();
            }
        });

        et.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String q = (s == null) ? "" : s.toString().trim();

                // Debounce
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);

                pendingSearch = () -> {
                    if (q.length() < 2) {
                        adapter.submit(null);
                        return;
                    }
                    queryPlaces(q);
                };

                handler.postDelayed(pendingSearch, 250);
            }
        });

        // Use current location
        TextView useCurrent = findViewById(R.id.btnUseMyCurrentLocation);
        useCurrent.setOnClickListener(v -> useMyCurrentLocation());
    }

    private void queryPlaces(String query) {
        FindAutocompletePredictionsRequest request =
                FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setSessionToken(sessionToken)
                        // Optional: limit to Israel to reduce noise
                        .setCountries("IL")
                        .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    adapter.submit(response.getAutocompletePredictions());
                })
                .addOnFailureListener(e -> {
                    adapter.submit(null);
                    Toast.makeText(this,
                            "Places autocomplete failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onPredictionClicked(AutocompletePrediction prediction) {
        FetchPlaceRequest request = FetchPlaceRequest.builder(
                prediction.getPlaceId(),
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        ).setSessionToken(sessionToken).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();

                    String name = place.getName();
                    String address = place.getAddress();

                    double lat = 0, lng = 0;
                    if (place.getLatLng() != null) {
                        lat = place.getLatLng().latitude;
                        lng = place.getLatLng().longitude;
                    }

                    Intent data = new Intent();
                    data.putExtra(EXTRA_LOCATION_NAME, name);
                    data.putExtra(EXTRA_LOCATION_ADDRESS, address);
                    data.putExtra(EXTRA_LOCATION_LAT, lat);
                    data.putExtra(EXTRA_LOCATION_LNG, lng);

                    setResult(Activity.RESULT_OK, data);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch place: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void useMyCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        Toast.makeText(this, "Couldn't get location. Try enabling GPS.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    returnLocationFromGeocoder(loc);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void returnLocationFromGeocoder(@NonNull Location loc) {
        String name = "Current location";
        String address = "";

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                // best-effort readable label
                String locality = a.getLocality();
                String admin = a.getAdminArea();
                String country = a.getCountryName();

                if (locality != null) name = locality;
                address = (a.getAddressLine(0) != null) ? a.getAddressLine(0) : "";

                // fallback if locality missing
                if ((name == null || name.trim().isEmpty()) && admin != null) name = admin;
                if ((name == null || name.trim().isEmpty()) && country != null) name = country;
            }
        } catch (IOException ignored) {}

        Intent data = new Intent();
        data.putExtra(EXTRA_LOCATION_NAME, name);
        data.putExtra(EXTRA_LOCATION_ADDRESS, address);
        data.putExtra(EXTRA_LOCATION_LAT, loc.getLatitude());
        data.putExtra(EXTRA_LOCATION_LNG, loc.getLongitude());

        setResult(Activity.RESULT_OK, data);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) granted &= (r == PackageManager.PERMISSION_GRANTED);
            if (granted) {
                useMyCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}