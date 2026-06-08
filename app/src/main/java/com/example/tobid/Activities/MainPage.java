package com.example.tobid.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainPage extends AppCompatActivity implements View.OnClickListener {
    // Firebase instances
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private TextView tvUsername;
    private ImageButton BTNnewSale;


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
        BTNnewSale = findViewById(R.id.BTNnewSale);
        BTNnewSale.setOnClickListener(this);

         tvUsername = findViewById(R.id.tvUsername);
        // Initialize Firebase and Auth instances
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            fetchAndDisplayUsername();
        }



    }


    private void fetchAndDisplayUsername() {
        String currentUserId = user.getUid();
        myRef = database.getReference("Users").child(currentUserId);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    if (username != null) {
                        tvUsername.setText(username);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read username", error.toException());
            }
        });
    }




    @Override
    public void onClick(View v) {
        if(v == BTNnewSale){
            Intent i = new Intent(this, CreateSaleActivity.class);
            startActivity(i);
        }

    }


}
