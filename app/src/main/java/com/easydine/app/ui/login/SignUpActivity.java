package com.easydine.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.easydine.app.MainActivity;
import com.easydine.app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.*;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordAgain;
    private EditText etPhoneNumber;
    private Button btnSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Connect Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordAgain = findViewById(R.id.etPasswordAgain);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSignUp = findViewById(R.id.btnSignUp);

        // 3. Set Button Listener
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();
                String PasswordAgain = etPasswordAgain.getText().toString();
                String PhoneNumber = etPhoneNumber.getText().toString();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)
                        || TextUtils.isEmpty(PasswordAgain) || TextUtils.isEmpty(PhoneNumber)
                ) {
                    Toast.makeText(SignUpActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!TextUtils.equals(etPassword.getText().toString() , etPasswordAgain.getText().toString()))
                {
                    Toast.makeText(SignUpActivity.this, "Passwords does`nt match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 4. Create User in Firebase
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // --- NEW CODE STARTS HERE ---
                                    // 3. Create a User Object to save
                                    String userId = mAuth.getCurrentUser().getUid();
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("email", email);
                                    userMap.put("phoneNumber" , PhoneNumber);
                                    userMap.put("role", "customer");

                                    // 4. Save to Firestore: collection "users", document name = userId
                                    db.collection("users").document(userId)
                                            .set(userMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    // Only move to next screen if Database save is successful
                                                    Toast.makeText(SignUpActivity.this, "Account Created & Saved!", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(SignUpActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    // --- NEW CODE ENDS HERE ---
                                } else {
                                    // Failure
                                    Toast.makeText(SignUpActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}