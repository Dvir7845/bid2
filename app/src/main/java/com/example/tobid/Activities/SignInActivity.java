package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * The SignInPage activity handles user authentication via Firebase Authentication.
 * Users can sign in, navigate to the sign-up page, or reset their password.
 */
public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    // UI elements
    private EditText etEmail, etPassword;
    private TextView tvNotes;
    private Button btnSignIn, btnForgotPassword, btnSignup;

    // Firebase instances
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;

    /**
     * Called when the activity starts. If the user is already signed in, they are redirected to the main page.
     */
    @Override
    public void onStart() {
        super.onStart();

        // Check if the user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload(); // Reload the user to ensure their session is active

            // Redirect to the main page
            Intent i = new Intent(this, MainPage.class);
            startActivity(i);
        }
    }

    /**
     * Called when the activity is first created.
     * Initializes Firebase instances and UI components.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize FirebaseDatabase and FirebaseAuth
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvNotes = findViewById(R.id.tvNotes);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(this);

        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnForgotPassword.setOnClickListener(this);

        btnSignup = findViewById(R.id.btnSignup);
        btnSignup.setOnClickListener(this);
    }

    /**
     * Handles button clicks for sign-in, navigation to the sign-up page, and navigation to the forgot password page.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v == btnSignup) {
            // Navigate to the sign-up page
            Intent i = new Intent(this, SignUpActivity.class);
            startActivity(i);
        }

        if (v == btnForgotPassword) {
            // Navigate to the forgot password page
            Intent i = new Intent(this, ForgotPasswordActivity.class);
            startActivity(i);
        }

        if (v == btnSignIn) {
            // Retrieve user input
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                tvNotes.setText("Please enter both email and password.");
                Log.d("Signin", "Email or password is missing.");
                return;
            }

            // Attempt to sign in with Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign-in successful, get token and send to server for verification
                                mAuth.getCurrentUser().getIdToken(true).addOnSuccessListener(result -> {
                                    String idToken = result.getToken();

                                    // Build request and send to server
                                    Request request = new Request(Action.LOGIN);
                                    request.putData("idToken", idToken);

                                    ServerConnection server = ServerConnection.getInstance();
                                    server.sendRequest(request, new ServerCallback() {
                                        @Override
                                        public void onResponseReceived(Response response) {
                                            if (response.isSuccess()) {
                                                Log.d(TAG, "signInWithEmail:success");
                                                // Navigate to the main page
                                                Intent i = new Intent(getApplicationContext(), MainPage.class);
                                                startActivity(i);
                                            } else {
                                                // Response unsuccessful
                                                tvNotes.setText("Server side error. Please try again later");
                                            }
                                        }
                                    });


                                });

                            } else {
                                // Sign-in failed
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(SignInActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();

                                // Display an error message to the user
                                tvNotes.setText("Email or password may be incorrect.");
                                Log.d("Signin", "Email or password may be incorrect.");
                            }
                        }
                    });
        }
    }
}
