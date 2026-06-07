package com.example.tobid.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.tobid.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener{

    // UI components
    private Button btnChangePassword; // Button to initiate password reset or navigate to login
    private EditText etEmail; // EditText for user to input their email
    private TextView tvNotes; // TextView to display messages such as status updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize the email input field
        etEmail = findViewById(R.id.etEmail);

        // Initialize the TextView for displaying notes like "Email sent" or errors
        tvNotes = findViewById(R.id.tvNotes);

        // Initialize the button and set its OnClickListener
        // This button initially triggers the password reset process
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Check if the clicked view is the Change Password button
        if (v == btnChangePassword) {
            // Fetch the email entered by the user
            String email = etEmail.getText().toString();

            // If the button text is "Change Password", proceed to reset the password
            if (btnChangePassword.getText().toString().equals("Change Password")) {
                // Get FirebaseAuth instance to interact with Firebase Authentication
                FirebaseAuth auth = FirebaseAuth.getInstance();

                // Send a password reset email using Firebase Authentication
                auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // If the email is sent successfully, display a success message
                                    Log.d("ForgotPassword", "Email sent.");
                                    tvNotes.setText("Email sent. Please check your email to resume");

                                    // Change the button text to "Go to login screen" after the email is sent
                                    btnChangePassword.setText("Go to login screen");

                                } else {
                                    // If the email was incorrect, display an error message
                                    tvNotes.setText("Email incorrect. Please re-enter your email");
                                }
                            }
                        });
            } else {
                // If the email has already been sent, navigate to the Sign In page
                Intent i = new Intent(this, SignInActivity.class);
                startActivity(i);
            }
        }
    }
}
