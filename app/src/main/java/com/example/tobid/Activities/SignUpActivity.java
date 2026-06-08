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

import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.User;
import com.example.tobid.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jspecify.annotations.NonNull;

import java.util.Calendar;

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
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

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
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

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

            // Create a new account using Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Account creation successful
                                FirebaseUser user = mAuth.getCurrentUser();
                                User newUser = createUser(user); // Create a new user profile

                                // Create a welcome notification for the user
                                String notificationText = "Welcome to 2Bid! Start setting up your profile by exploring new biddings.";
                                Notification signUpNotification = new Notification(
                                        "2Bid-" + Calendar.getInstance().getTimeInMillis(),
                                        "2Bid", "2Bid", newUser.getImg(), notificationText);

                                // Save the notification in the database
                                myRef = database.getReference("/Users/" + mAuth.getUid() + "/notifications/" + signUpNotification.getId());
                                myRef.setValue(signUpNotification);

                                // Navigate to the sign-in page
                                startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                            } else {
                                // Account creation failed
                                displayMessage("Couldn't create account. Is your email correct?");
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
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

    /**
     * Creates a new user profile in the Firebase Realtime Database and uploads a default profile picture.
     *
     * @param user The FirebaseUser created during signup.
     */
    private User createUser(FirebaseUser user) {
        String uid = user.getUid();
        String email = user.getEmail();
        String username = etUsername.getText().toString();
        String phoneNumber = etPhoneNumber.getText().toString();

        // Define the default profile picture path
        String userImgPath = "/Users/" + uid + "/profilePicture.png";
        User newUser = new User(uid, email, phoneNumber, username, userImgPath);

        // Save the user profile in the database
        myRef = database.getReference("/Users/" + uid);
        myRef.setValue(newUser);

        // Upload the default profile picture
        StorageReference defaultPfpRef = storage.getReference("/DefaultPfp/DefaultPfp.png");
        defaultPfpRef.getBytes(1024 * 1024) // 1MB max
                .addOnSuccessListener(bytes -> {
                    StorageReference userPfpRef = storage.getReference(userImgPath);
                    UploadTask uploadTask = userPfpRef.putBytes(bytes);

                    uploadTask.addOnSuccessListener(taskSnapshot -> {
                        myRef = database.getReference("/Users/" + uid + "/img");
                        myRef.setValue(userImgPath);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this, "Couldn't create user, please try again", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Could not create user: Couldn't upload default profile picture.", e);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Couldn't fetch default profile picture", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to get default picture: ", e);
                });


        return newUser;
    }
}
