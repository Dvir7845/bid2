package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Response;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.example.tobid.DataModels.User;
import com.example.tobid.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.jspecify.annotations.NonNull;

/**
 * The SignUpPage activity allows users to register for the HangOut app.
 * It handles user input, validation, Firebase authentication, and creating a new user profile.
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    // UI elements
    private EditText etEmail, etUsername, etPhoneNumber, etPassword, etPasswordConfirm;
    private Button btnSignup;
    private TextView tvNotes;

    // Firebase instances
    private FirebaseAuth mAuth;

    /**
     * Called when the activity is first created.
     * Initializes UI components and Firebase instances.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        tvNotes = findViewById(R.id.tvNotes);
        btnSignup = findViewById(R.id.btnSignup);

        // Set up click listener for the signup button
        btnSignup.setOnClickListener(this);
    }

    /**
     * Handles button clicks for the activity.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v == btnSignup) {
            // Retrieve user inputs
            String email = etEmail.getText().toString();
            String username = etUsername.getText().toString();
            String phoneNumber = etPhoneNumber.getText().toString();
            String password = etPassword.getText().toString();
            String passwordConfirm = etPasswordConfirm.getText().toString();

            // Input validation
            if (email.isEmpty()) {
                displayMessage("Please enter an email.");
                return;
            } else if (username.isEmpty()) {
                displayMessage("Please enter a username.");
                return;
            } else if (phoneNumber.isEmpty()) {
                displayMessage("Please enter a phone number");
            } else if (password.isEmpty()) {
                displayMessage("Please enter a password.");
                return;
            } else if (!password.equals(passwordConfirm)) {
                displayMessage("Passwords must match.");
                return;
            }
            // Define the default profile picture path
            // Create a new account using Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                // Account creation failed
                                displayMessage("Couldn't create account. Is your email correct? or maybe your password is too short?(6 characters)");
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Account creation successful
                                String uid = mAuth.getCurrentUser().getUid();
                                String userImgPath = "/Users/" + uid + "/profilePicture.png";
                                User newUser = new User(uid, email, phoneNumber, username, userImgPath); // Create a new user profile

                                Request request = new Request(Action.REGISTER);
                                request.putData("userObject", newUser);

                                // Send request to server and handle appropriately
                                ServerConnection server = ServerConnection.getInstance();
                                server.sendRequest(request, new ServerCallback() {
                                    @Override
                                    public void onResponseReceived(Response response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (response != null && response.isSuccess()) {
                                                    // Navigate to the sign-in page
                                                    startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                                }
                                                else {
                                                    displayMessage("Server side error, couldn't create account. Please try again later");
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
        }
    }

    /**
     * Displays a message in the notes TextView and logs it.
     *
     * @param message The message to display.
     */
    private void displayMessage(String message) {
        tvNotes.setText(message);
        Log.d("Signup", message);
    }
}
