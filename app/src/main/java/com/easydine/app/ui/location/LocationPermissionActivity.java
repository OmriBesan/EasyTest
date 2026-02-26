package com.easydine.app.ui.location;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.easydine.app.R;
import com.easydine.app.ui.restaurant.RestaurantListActivity;
import com.easydine.app.utils.LocationHelper;
import com.google.android.material.button.MaterialButton;

public class LocationPermissionActivity extends AppCompatActivity {

    private LocationHelper locationHelper;

    private ActivityResultLauncher<Intent> manualLocationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_what_is_your_location);
        // Skip if location already chosen
        if (getSharedPreferences("easydine_prefs", MODE_PRIVATE).getBoolean("location_set", false)) {
            goNextAfterLocation();
            return;
        }

        locationHelper = new LocationHelper(this);

        // ✅ Register launcher ONCE (before using it)
        manualLocationLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

                            Intent data = result.getData();
                            String name = data.getStringExtra(EnterLocationManuallyActivity.EXTRA_LOCATION_NAME);
                            String address = data.getStringExtra(EnterLocationManuallyActivity.EXTRA_LOCATION_ADDRESS);
                            double lat = data.getDoubleExtra(EnterLocationManuallyActivity.EXTRA_LOCATION_LAT, 0);
                            double lng = data.getDoubleExtra(EnterLocationManuallyActivity.EXTRA_LOCATION_LNG, 0);
                            String toastName = (name != null && !name.trim().isEmpty()) ? name : "your location";
                            saveChosenLocation(name, address, lat, lng);
                            Toast.makeText(this, "Location set to " + toastName, Toast.LENGTH_SHORT).show();

                            goNextAfterLocation();
                        }
                );

        MaterialButton btnAllow = findViewById(R.id.btnAllowLocation);
        TextView tvManual = findViewById(R.id.tvEnterLocationManually);

        //  Allow location access button
        btnAllow.setOnClickListener(v ->
                locationHelper.getCurrentLocation(location -> {

                    goNextAfterLocation();
                })
        );

        //  Manual location click -> launch manual screen
        tvManual.setOnClickListener(v ->
                manualLocationLauncher.launch(new Intent(this, EnterLocationManuallyActivity.class))
        );
    }

    private void goNextAfterLocation() {
        Intent i = new Intent(this, RestaurantListActivity.class);
        i.putExtra("location_source", "manual");
        startActivity(i);
        finish();
    }

    private void saveChosenLocation(String name, String address, double lat, double lng) {
        getSharedPreferences("easydine_prefs", MODE_PRIVATE)
                .edit()
                .putString("location_name", name)
                .putString("location_address", address)
                .putFloat("location_lat", (float) lat)
                .putFloat("location_lng", (float) lng)
                .putBoolean("location_set", true)
                .apply();
    }

    // LocationHelper requests permission with code 100
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                locationHelper.getCurrentLocation(loc -> goNextAfterLocation());
            } else {
                // Permission denied → user can still proceed manually
                // (Don't auto-go home here unless you want that behavior)
            }
        }
    }
}