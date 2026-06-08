package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainPage extends AppCompatActivity implements View.OnClickListener {
    // Firebase instances
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private TextView tvUsername;
    private ImageButton btnNewSale, ibHomeButton, ibNotifications, ibBiddingHistory;;
    private CircleImageView ivPfp;


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
        btnNewSale = findViewById(R.id.BTNnewSale);
        btnNewSale.setOnClickListener(this);

        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        tvUsername = findViewById(R.id.tvUsername);
        // Initialize Firebase and Auth instances
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            fetchAndDisplayUsername();
        }

        // Initialize profile picture
        ivPfp = findViewById(R.id.ivPfp);
        ivPfp.setOnClickListener(this);
        String uid = mAuth.getUid();
        fetchAndDisplayProfilePicture(uid);

    }

    private void fetchAndDisplayProfilePicture(String uid) {
        // Fetch and display profile picture from Firebase Storage
        myRef = database.getReference("/Users/" + uid + "/img");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference(dataSnapshot.getValue(String.class));

                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(MainPage.this).load(uri).into(ivPfp);
                }).addOnFailureListener(exception -> {
                    Log.e("FirebaseStorage", "Failed to get download URL: " + exception.getMessage());
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
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
        if(v == btnNewSale){
            Intent i = new Intent(this, CreateSaleActivity.class);
            startActivity(i);
        } else if (v == ivPfp) {
            Intent i = new Intent(this, ChangeProfilePage.class);
            startActivity(i);
        } else if (v == ibBiddingHistory) {
            Intent i = new Intent(this, SalesHistoryActivity.class);
            startActivity(i);
        }

        else if (v == ibHomeButton) {
            //Intent i = new Intent(this, MainPage.class);
            //startActivity(i);
        }

        else if (v == ibNotifications) {
            Intent i = new Intent(this, NotificationsActivity.class);
            startActivity(i);
        }

    }


}
