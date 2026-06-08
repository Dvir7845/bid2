package com.example.tobid.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainPage extends AppCompatActivity implements View.OnClickListener {
    // Firebase instances
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private ImageButton ibNewBid, ibBidHistory, ibHomeButton, ibNotifications;
    private TextView tvSearchBar;
    private Spinner spCategory;


    // Method to reload user data when the app is resumed
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload();
        }
    }

    // Method to initialize the activity and UI elements
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        // Initialize Firebase and Auth instances
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // Initialize buttons
        ibNewBid = findViewById(R.id.ibNewBid);
        ibBidHistory = findViewById(R.id.ibBidHistory);
        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibNotifications = findViewById(R.id.ibNotifications);

        spCategory = findViewById(R.id.spCategory);
        tvSearchBar = findViewById(R.id.tvSearchBar);
    }


    @Override
    public void onClick(View v) {
        if (v == ibHomeButton) {
            return; // Already in home
        }
        else if (v == ibBidHistory) {
            Intent i = new Intent(this, SalesHistoryActivity.class);
            startActivity(i);
        }
        else if (v == ibNotifications) {
            Intent i = new Intent(this, NotificationsActivity.class);
            startActivity(i);
        }
        else if (v == ibNewBid) {
            Intent i = new Intent(this, CreateSaleActivity.class);
            startActivity(i);
        }

    }
}
