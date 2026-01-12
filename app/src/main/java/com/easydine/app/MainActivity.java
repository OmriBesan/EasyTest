package com.easydine.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.easydine.app.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // You can make this layout just a ProgressBar

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            // 1. Not Logged In -> Go to Login
            sendToLogin();
        } else {
            // 2. Logged In -> Check if Owner or Customer
            checkUserType(user);
        }
    }

    private void checkUserType(FirebaseUser user) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("restaurants")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // --- IS OWNER ---
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Intent intent = new Intent(this, com.easydine.app.ui.owner.ManageScheduleActivity.class);
                        intent.putExtra("RESTAURANT_ID", doc.getId());
                        startActivity(intent);
                    } else {
                        // --- IS CUSTOMER (Regular User) ---
                        // This is the new part! Send them to the List immediately.
                        Intent intent = new Intent(this, com.easydine.app.ui.restaurant.RestaurantListActivity.class);
                        startActivity(intent);
                    }
                    finish(); // Close MainActivity so "Back" button exits the app, not back here.
                });
    }

    private void sendToLogin() {
        Intent intent = new Intent(this, com.easydine.app.ui.login.LoginActivity.class); // Check your package path!
        startActivity(intent);
        finish();
    }
}