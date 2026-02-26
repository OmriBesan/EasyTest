package com.easydine.app.ui.login; // Make sure this matches your actual package name

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private com.google.android.material.button.MaterialButton btnLogin;
    private TextView tvGoToSignUp;
    private FirebaseAuth mAuth;

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
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // check if the user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("restaurants")
                    .whereEqualTo("userID", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Intent intent;
                        if (!querySnapshot.isEmpty()) {
                            // CASE 1: THEY ARE AN OWNER
                            String myRestaurantId = querySnapshot.getDocuments().get(0).getId();
                            intent = new Intent(LoginActivity.this, com.easydine.app.ui.owner.ManageScheduleActivity.class);
                            intent.putExtra("RESTAURANT_ID", myRestaurantId);
                        } else {
                            // CASE 2: THEY ARE A REGULAR CUSTOMER
                            intent = new Intent(LoginActivity.this, RestaurantListActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnSignIn);
        tvGoToSignUp = findViewById(R.id.tvSignUp);

        //GOOGLE SIGN-IN SETUP
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("531335500910-66bt87u76ulli2a3verl0see5ln2557j.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- ADDED: GOOGLE BUTTON CLICK LISTENER ---
        // MAKE SURE YOU HAVE A BUTTON WITH id "@+id/btnGoogleSignIn" IN YOUR XML!
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        //login using email/password
        makeSignUpClickable(tvGoToSignUp);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, RestaurantListActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
    private void makeSignUpClickable(TextView tv) {
        String full = "Don’t have an account? Sign Up";
        String clickable = "Sign Up";

        int start = full.indexOf(clickable);
        int end = start + clickable.length();

        android.text.SpannableString ss = new android.text.SpannableString(full);

        // color "Sign Up"
        ss.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#0F172A")),
                start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // clickable only on "Sign Up"
        ss.setSpan(new android.text.style.ClickableSpan() {
            @Override
            public void onClick(android.view.View widget) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }

            @Override
            public void updateDrawState(android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ss);
        tv.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        tv.setHighlightColor(android.graphics.Color.TRANSPARENT);
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
                        startActivity(new Intent(LoginActivity.this, RestaurantListActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "Firebase Auth Failed", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}