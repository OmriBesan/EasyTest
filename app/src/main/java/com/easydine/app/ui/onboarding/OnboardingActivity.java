package com.easydine.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.easydine.app.R;
import com.easydine.app.ui.location.LocationPermissionActivity;
import com.easydine.app.ui.login.OnboardingAdapter;


public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS = "easydine_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip onboarding if already completed
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (sp.getBoolean(KEY_ONBOARDING_DONE, false)) {
            goToLocation();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        ViewPager2 pager = findViewById(R.id.viewPagerOnboarding);

        int[] pages = new int[]{
                R.layout.item_onboarding_page1,
                R.layout.item_onboarding_page2,
                R.layout.item_onboarding_page3
        };

        OnboardingAdapter adapter = new OnboardingAdapter(this, pages, () -> {
            sp.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
            goToLocation();
        });

        pager.setAdapter(adapter);
    }

    private void goToLocation() {
        Intent i = new Intent(this, LocationPermissionActivity.class);
        startActivity(i);
        finish();
    }
}