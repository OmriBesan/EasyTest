package com.easydine.app.ui.login; // Make sure this matches your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.easydine.app.MainActivity;
import com.easydine.app.R;
import com.easydine.app.ui.restaurant.RestaurantListActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToSignUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // The user is logged in. Now we check: Are they an Owner or a Customer?
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("restaurants")
                    .whereEqualTo("userID", currentUser.getUid()) // Check if they own a restaurant
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Intent intent;

                        if (!querySnapshot.isEmpty()) {
                            // CASE 1: THEY ARE AN OWNER (Found a restaurant with their ID)
                            // Get the Restaurant ID from the document we found
                            String myRestaurantId = querySnapshot.getDocuments().get(0).getId();

                            intent = new Intent(LoginActivity.this, com.easydine.app.ui.owner.ManageScheduleActivity.class);
                            intent.putExtra("RESTAURANT_ID", myRestaurantId);
                        } else {
                            // CASE 2: THEY ARE A REGULAR CUSTOMER (No restaurant found)
                            intent = new Intent(LoginActivity.this, com.easydine.app.ui.restaurant.RestaurantListActivity.class);
                        }

                        // Clear the back stack so they can't press "Back" to return to Login
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Close LoginActivity
                    });

            return; // Stop the rest of the code from running while we redirect
        }

        // Connect Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // Go to Sign Up Page
        tvGoToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        // Handle Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Success
                                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, RestaurantListActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Failure
                                    Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}