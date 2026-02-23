package com.easydine.app.ui.login;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.easydine.app.MainActivity;
import com.easydine.app.R;
import com.easydine.app.ui.restaurant.RestaurantListActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.GoogleAuthProvider;



import java.util.*;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordAgain;
    private EditText etPhoneNumber;
    private Button btnSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient mGoogleSignInClient; //for Google Login

    //Google Sign-In Launcher (Listens for the Google account popup to close)
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


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

        //GOOGLE SIGN-IN SETUP
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("531335500910-66bt87u76ulli2a3verl0see5ln2557j.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- ADDED: GOOGLE BUTTON CLICK LISTENER ---
        // MAKE SURE YOU HAVE A BUTTON WITH id "@+id/btnGoogleSignIn" IN YOUR XML!
        findViewById(R.id.btnGoogleSignUp).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

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

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success!
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = user != null ? user.getDisplayName() : "User";
                        Toast.makeText(this, "Welcome " + name, Toast.LENGTH_SHORT).show();

                        // Go to the main restaurant list
                        startActivity(new Intent(SignUpActivity.this, RestaurantListActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "Firebase Auth Failed", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}