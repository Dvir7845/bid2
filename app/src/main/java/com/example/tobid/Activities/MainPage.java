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
import com.example.tobid.DataModels.User;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainPage extends AppCompatActivity implements View.OnClickListener {
    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private User userData;
    private TextView tvUsername, tvOngoingBidsText;
    private Spinner spCategory;
    private EditText etSearchBar;
    private Button btnSearch;
    private ImageButton btnNewBid, ibHomeButton, ibNotifications, ibBiddingHistory;;
    private CircleImageView ivPfp;

    private ArrayList<Bid> ongoingBids, futureBids;
    private RecyclerView rvOngoingBids, rvFutureBids;
    private View.OnClickListener ongoingBidsOnItemClickListener, futureBidsOnItemClickListener;
    private BidAdapter ongoingBidsAdapter, futureBidsAdapter;

    // Security variables to prevent infinite Spinner selection loops
    private boolean isSpinnerInitializing = true;
    private int lastSelectedCategoryPosition = -1;



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
        ivPfp = findViewById(R.id.ivPfp);
        ivPfp.setOnClickListener(this);

        // Initialize Firebase and Auth instances
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            fetchAndDisplayUsernameAndPicture();
        }

        // Fetch and display categories from the db
        spCategory = findViewById(R.id.spCategory);
        setupSpinnerListener();
        fetchAndDisplayCategories();

        // Fetch and display ongoing bids amount
        tvOngoingBidsText = findViewById(R.id.tvOngoingBidsText);
        fetchAndDisplayOngoingBidsAmount();

        // Initialize Adapter and Display bids
        initializeAndDisplayBids();
    }

    private void fetchAndDisplayOngoingBidsAmount() {
        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_AMOUNT_OF_ONGOING_BIDS);
        request.putData("uid", mAuth.getUid());

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            int amount = (int) response.getData("amountOfOngoingBids");
                            tvOngoingBidsText.setText(amount + " ongoing bids");
                        } else {
                            Toast.makeText(MainPage.this, "Unable to get amount of ongoing bids", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
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
                // Safety Check 1: Handle the automatic trigger when setting the adapter for the first time
                if (isSpinnerInitializing) {
                    isSpinnerInitializing = false;
                    lastSelectedCategoryPosition = position;
                    // We STILL want to fetch bids the first time the app opens!
                    displayOngoingBidsWithFilters();
                    return;
                }

                // Safety Check 2: Ignore if Android randomly triggers the same position due to UI layout changes
                if (lastSelectedCategoryPosition == position) {
                    return;
                }

                // Update the tracked position and fetch new bids based on user selection
                lastSelectedCategoryPosition = position;
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
                        Log.d("BidDebug", "Raw server data size: " + (receivedBids == null ? "NULL" : receivedBids.size()));

                        receivedBids = filterBidsByName(receivedBids, bidNameFilter);
                        Log.d("BidDebug", "Server returned: " + (receivedBids != null ? receivedBids.size() : "null") + " bids");
                        Log.d("BidDebug", "After filter: " + (receivedBids == null ? "NULL" : receivedBids.size()));


                        Log.d("BidDebug", "After filter: " + receivedBids.size() + " bids");

                        ongoingBids.addAll(receivedBids);

                        receivedBids = (ArrayList<Bid>) response.getData("futureBids");
                        receivedBids = filterBidsByName(receivedBids, bidNameFilter);

                        futureBids.addAll(receivedBids);

                        System.out.println("ongoing" + ongoingBids.toString());
                        System.out.println("future" + futureBids.toString());

                        Collections.shuffle(ongoingBids);
                        Collections.shuffle(futureBids);
                        Log.d("BidDebug", "Final list size passed to Adapter: " + ongoingBids.size());
                        ongoingBidsAdapter.notifyDataSetChanged();
                        futureBidsAdapter.notifyDataSetChanged();
                    } else {
                        // Toast.makeText(MainPage.this, "Failed getting bids for display", Toast.LENGTH_SHORT).show();
                        String errorMsg = (response != null && response.getMessage() != null) ? response.getMessage() : "Unknown Server Error";
                        Toast.makeText(MainPage.this, "Server error: " + errorMsg, Toast.LENGTH_LONG).show();
                        System.out.println("🚨 SERVER REJECTED: " + errorMsg);
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
        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_CATEGORIES);

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            ArrayList<String> categories = (ArrayList<String>) response.getData("categories");

                            categories.add(0, "All"); // Add all category when displaying for no category filter

                            ArrayAdapter<String> spinnerAdapter =
                                    new ArrayAdapter<>(MainPage.this, android.R.layout.simple_spinner_item, categories);
                            spinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
                            isSpinnerInitializing = true;
                            spCategory.setAdapter(spinnerAdapter);
                        } else {
                            Toast.makeText(MainPage.this, "Category fetch failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void fetchAndDisplayUsernameAndPicture() {
        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_USER_BY_ID);
        request.putData("uid", mAuth.getUid());

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            userData = (User) response.getData("user");
                            String username = userData.getUsername();
                            tvUsername.setText(username);

                            String imagePath = userData.getImg();
                            fetchAndDisplayProfilePicture(imagePath);
                        } else {
                            Toast.makeText(MainPage.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
    private void fetchAndDisplayProfilePicture(String imagePath) {
        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_IMAGE_BY_PATH);
        request.putData("imagePath", imagePath);

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            String imageUrl = (String) response.getData("imageUrl");
                            System.out.println(imageUrl);
                            // Use Glide to load the image into the ImageView
                            ivPfp.post(new Runnable() {
                                @Override
                                public void run() {
                                    Glide.with(ivPfp.getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_pfp)
                                            .into(ivPfp);
                                }
                            });
                        } else {
                            Toast.makeText(MainPage.this, "Failed to get profile picture", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(v == btnNewBid){
            Intent i = new Intent(this, CreateBidActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else if (v == ivPfp) {
            Intent i = new Intent(this, ChangeProfilePage.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else if (v == ibBiddingHistory) {
            Intent i = new Intent(this, BidsHistoryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        else if (v == ibHomeButton) {
            //Intent i = new Intent(this, MainPage.class);
            //startActivity(i);
        }
        else if (v == ibNotifications) {
            Intent i = new Intent(this, NotificationsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        else if (v == btnSearch) {
            displayOngoingBidsWithFilters();
        }
    }


}
