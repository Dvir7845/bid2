package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.helper.widget.MotionEffect;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.tobid.Adapters.BidAdapter;
import com.example.tobid.DataModels.Sale;
import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainPage extends AppCompatActivity implements View.OnClickListener {
    // Firebase instances
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private TextView tvUsername;
    private Spinner spCategory;
    private EditText etSearchBar;
    private Button btnSearch;
    private ImageButton btnNewSale, ibHomeButton, ibNotifications, ibBiddingHistory;;
    private CircleImageView ivPfp;

    private ArrayList<Sale> ongoingBids;
    private RecyclerView rvOngoingBids;
    private View.OnClickListener onItemClickListener;
    private BidAdapter ongoingBidAdapter;


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
        btnNewSale = findViewById(R.id.btnNewSale);
        btnNewSale.setOnClickListener(this);

        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);

        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        etSearchBar = findViewById(R.id.etSearchBar);

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

        // Fetch and display categories from the db
        spCategory = findViewById(R.id.spCategory);
        setupSpinnerListener();
        fetchAndDisplayCategories();

        // Initialize Adapter and Display ongoing bids
        ongoingBids = new ArrayList<>();
        onItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = ((RecyclerView.ViewHolder) view.getTag()).getAdapterPosition();
                Sale bid = ongoingBids.get(position);
                Intent i = new Intent(MainPage.this, DisplaySaleActivity.class);
                i.putExtra("Sale", bid);
                startActivity(i);
            }
        };
        ongoingBidAdapter = new BidAdapter(ongoingBids);
        ongoingBidAdapter.setmOnClickListener(onItemClickListener);

        rvOngoingBids = findViewById(R.id.rvOngoingBids);

        rvOngoingBids.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvOngoingBids.setAdapter(ongoingBidAdapter);
    }

    private void setupSpinnerListener() {
        spCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Refresh bids filter
                displayOngoingBidsWithFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // noop
            }
        });
    }

    private void displayOngoingBidsWithFilters() {
        String bidNameFilter = etSearchBar.getText().toString().trim().toLowerCase();
        Object selectedCategory = spCategory.getSelectedItem();
        String categoryFilter = (selectedCategory != null) ? selectedCategory.toString() : "All";

        if (categoryFilter.equals("All")) {
            myRef = database.getReference().child("Bids");
        } else {
            myRef = database.getReference().child("Bids").child(categoryFilter);
        }

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ongoingBids.clear();

                if (!snapshot.exists()) {
                    ongoingBidAdapter.notifyDataSetChanged();
                    return;
                }

                if (categoryFilter.equals("All")) {
                    // For each category
                    for (DataSnapshot bidsCategorySnapshot : snapshot.getChildren()) {
                        for (DataSnapshot bidSnapshot : bidsCategorySnapshot.getChildren()) {
                            processAndAddBid(bidSnapshot, bidNameFilter);
                        }
                    }
                } else {
                    // A specific category is selected
                    for (DataSnapshot bidSnapshot : snapshot.getChildren()) {
                        processAndAddBid(bidSnapshot, bidNameFilter);
                    }
                }

                // Shuffle ongoing bids so won't be in category order
                Collections.shuffle(ongoingBids);
                ongoingBidAdapter.notifyDataSetChanged();
                System.out.println(ongoingBids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(MotionEffect.TAG, "Failed to read value.", error.toException());
            }
        });

    }

    private void processAndAddBid(DataSnapshot bidSnapshot, String bidNameFilter) {
        Sale bid = bidSnapshot.getValue(Sale.class);
        if (bid == null) return;
        else if (bid.getItem().getSellerUID().equals(mAuth.getUid())) return;
        else if (!bidNameFilter.isEmpty()) {
            String itemName = bid.getItem().getItemName().toLowerCase();
            if (!itemName.contains(bidNameFilter))
                return;
        }
        ongoingBids.add(bid);
        System.out.println(ongoingBids);
    }

    private void fetchAndDisplayCategories() {
        myRef = database.getReference();
        myRef.child("Categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<ArrayList<String>> type = new GenericTypeIndicator<ArrayList<String>>() {};
                ArrayList<String> categories = snapshot.getValue(type);
                categories.add(0, "All"); // Add all category when displaying for no category filter

                ArrayAdapter<String> spinnerAdapter =
                        new ArrayAdapter<>(MainPage.this, android.R.layout.simple_spinner_item, categories);
                spinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
                spCategory.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read categories", error.toException());
            }
        });
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
        else if (v == btnSearch) {
            displayOngoingBidsWithFilters();
        }
    }


}
