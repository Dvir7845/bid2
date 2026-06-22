package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tobid.Adapters.BidAdapter;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
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
    private ImageButton btnNewBid, ibHomeButton, ibNotifications, ibBiddingHistory;;
    private CircleImageView ivPfp;

    private ArrayList<Bid> ongoingBids, futureBids;
    private RecyclerView rvOngoingBids, rvFutureBids;
    private View.OnClickListener ongoingBidsOnItemClickListener, futureBidsOnItemClickListener;
    private BidAdapter ongoingBidsAdapter, futureBidsAdapter;


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
        btnNewBid = findViewById(R.id.btnNewBid);
        btnNewBid.setOnClickListener(this);

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

        // Initialize Adapter and Display bids
        initializeAndDisplayBids();
    }

    private void initializeAndDisplayBids() {
        ongoingBids = new ArrayList<>();
        futureBids = new ArrayList<>();

        ongoingBidsOnItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = ((RecyclerView.ViewHolder) view.getTag()).getAdapterPosition();
                Bid bid = ongoingBids.get(position);
                Intent i = new Intent(MainPage.this, DisplayBidActivity.class);
                i.putExtra("Bid", bid);
                startActivity(i);
            }
        };
        futureBidsOnItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = ((RecyclerView.ViewHolder) view.getTag()).getAdapterPosition();
                Bid bid = futureBids.get(position);
                Intent i = new Intent(MainPage.this, DisplayBidActivity.class);
                i.putExtra("Bid", bid);
                startActivity(i);
            }
        };

        ongoingBidsAdapter = new BidAdapter(ongoingBids);
        ongoingBidsAdapter.setmOnClickListener(ongoingBidsOnItemClickListener);

        futureBidsAdapter = new BidAdapter(futureBids);
        futureBidsAdapter.setmOnClickListener(futureBidsOnItemClickListener);

        rvOngoingBids = findViewById(R.id.rvOngoingBids);
        rvFutureBids = findViewById(R.id.rvFutureBids);

        rvOngoingBids.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvOngoingBids.setAdapter(ongoingBidsAdapter);

        rvFutureBids.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFutureBids.setAdapter(futureBidsAdapter);
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

        ServerConnection server = ServerConnection.getInstance();

        Request request = new Request(Action.GET_ALL_BIDS_IN_CATEGORY);
        request.putData("Category", selectedCategory);

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(() -> {
                    if (response != null && response.isSuccess()) {
                        ongoingBids.clear();
                        futureBids.clear();

                        ArrayList<Bid> receivedBids = (ArrayList<Bid>) response.getData("ongoingBids");
                        receivedBids = filterBidsByName(receivedBids, bidNameFilter);

                        ongoingBids.addAll(receivedBids);

                        receivedBids = (ArrayList<Bid>) response.getData("futureBids");
                        receivedBids = filterBidsByName(receivedBids, bidNameFilter);

                        futureBids.addAll(receivedBids);

                        System.out.println("ongoing" + ongoingBids.toString());
                        System.out.println("future" + futureBids.toString());

                        Collections.shuffle(ongoingBids);
                        Collections.shuffle(futureBids);

                        ongoingBidsAdapter.notifyDataSetChanged();
                        futureBidsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MainPage.this, "Failed getting bids for display", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private ArrayList<Bid> filterBidsByName(ArrayList<Bid> receivedBids, String bidNameFilter) {
        ArrayList<Bid> filteredBids = new ArrayList<>();
        for (Bid bid : receivedBids) {
            System.out.println("Bid filter:" + bidNameFilter + " Bid name: " + bid.getItem().getItemName());
            Item item = bid.getItem();
            if (item.getSellerUID().equals(mAuth.getUid()))
                continue;
            else if (!bidNameFilter.isEmpty()) {
                String itemName = item.getItemName().toLowerCase();
                if (!itemName.contains(bidNameFilter))
                    continue;
            }

            filteredBids.add(bid);
        }

        return filteredBids;
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
        if(v == btnNewBid){
            Intent i = new Intent(this, CreateBidActivity.class);
            startActivity(i);
        } else if (v == ivPfp) {
            Intent i = new Intent(this, ChangeProfilePage.class);
            startActivity(i);
        } else if (v == ibBiddingHistory) {
            Intent i = new Intent(this, BidsHistoryActivity.class);
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
