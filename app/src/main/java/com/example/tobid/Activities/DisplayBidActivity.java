package com.example.tobid.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for displaying a single bid.
 */
public class DisplayBidActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView tvItemName, tvCategory, tvCurrentPrice, tvTimer, tvDetails ,tvSellerPhone;
    private EditText etBidAmount;
    private Button btnBid, btnAuto, btnBuy;
    private ImageView imageView1, imageView2, imageView3;
    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;
    private Bid bid;
    private FirebaseAuth mAuth;
    private String saleId, saleCategory;
    private boolean isExpired;
    private final  long second = 1000 , minute = 60 * second , hour = 60 * minute , day = 24 * hour;



    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display_bid);

        initViews();

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();

        // Get bid data
        Intent intent = getIntent();

        bid = (Bid) intent.getSerializableExtra("Bid");
        if (bid == null) {
            Toast.makeText(DisplayBidActivity.this, "No Data passed on the bid to display", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(DisplayBidActivity.this, MainPage.class);
            startActivity(i);
        }

        Item item = bid.getItem();
        saleId = bid.getBidId();
        saleCategory = item.getCategory();
        // Set bid data to views
        tvItemName.setText(bid.getItem().getItemName());
        tvCategory.setText(bid.getItem().getCategory());
        tvDetails.setText(bid.getItem().getItemDescription());
        tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", bid.getHighestOfferedBid()));


        // Load images
        ServerConnection server = ServerConnection.getInstance();
        // Send requests for image URLs
        sendRequestForImageAndDisplay(server, item.getStoragePathToImg1(), imageView1); // Get image 1
        sendRequestForImageAndDisplay(server, item.getStoragePathToImg2(), imageView2); // Get image 2
        sendRequestForImageAndDisplay(server, item.getStoragePathToImg3(), imageView3); // Get image 3
        // Set buy now button visibility
        if (bid.isHasMaximumPrice()) {
            btnBuy.setVisibility(View.VISIBLE);
            btnBuy.setText("BUY IT NOW FOR $" + bid.getMaximumPrice());
        } else {
            btnBuy.setVisibility(View.GONE);
        }

        // If the user is the creator of the bid
        if(mAuth.getUid().equals(bid.getItem().getSellerUID())){
            tvDetails.setText("This is your auction. You cannot place bids on your own items.");
            tvDetails.setVisibility(View.VISIBLE);
           disableBidding();//disable bidding buttons
        }

        // Check if the bid is expired
        long currentTime = Calendar.getInstance().getTimeInMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        isExpired = false;
        try {
            Date endDate = dateFormat.parse(bid.getEndDate());
            if (endDate != null && currentTime >= endDate.getTime()+day) {//add  day to include the current day
                isExpired = true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Check if the bid is a future bid
        try {
            Date startDate = dateFormat.parse(bid.getStartDate());

            if (startDate != null && currentTime < startDate.getTime()) {
             disableBidding();//disable bidding buttons to prevent bidding before the start date
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Check if the user is the seller or the leading bidder
        String currentUid = mAuth.getUid();
        if ((isExpired || (bid.getHighestOfferedBid() >= bid.getMaximumPrice()) && currentUid != null)) {
            if (currentUid.equals(bid.getItem().getSellerUID()) || currentUid.equals(bid.getLeadingBidderId())) {
                fetchAndShowSellerPhone();//show seller phone to contact seller
            }
        }

        startCountdown(bid.getEndDate());// Start the countdown timer

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void sendRequestForImageAndDisplay(ServerConnection server, String imagePath, ImageView ivToDisplayIn) {
        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }

        Request request = new Request(Action.GET_IMAGE_BY_PATH);
        request.putData("imagePath", imagePath);
        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                if (response != null && response.isSuccess()) {
                    String imageUrl = (String) response.getData("imageUrl");
                    // Use Glide to load the image into the ImageView
                    ivToDisplayIn.post(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(ivToDisplayIn.getContext())
                                    .load(imageUrl)
                                    .into(ivToDisplayIn);
                        }
                    });
                } else {
                    ivToDisplayIn.post(() -> {
                        if (ivToDisplayIn == imageView1 && response != null) {
                            System.out.println("Error when loading first bid image. " + response.getMessage());
                        } else {
                            ivToDisplayIn.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void initViews() { // Initialize views
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        tvTimer = findViewById(R.id.tvTimer);
        tvDetails = findViewById(R.id.tvDetails);
        tvSellerPhone = findViewById(R.id.tvSellerPhone);
        etBidAmount = findViewById(R.id.etBidAmount);
        btnBid = findViewById(R.id.btnBid);
        btnAuto = findViewById(R.id.btnAuto);
        btnBuy = findViewById(R.id.btnBuy);
        imageView1 = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        imageView3 = findViewById(R.id.imageView3);
        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        btnBid.setOnClickListener(this);
        btnAuto.setOnClickListener(this);
        btnBuy.setOnClickListener(this);
        ibHomeButton.setOnClickListener(this);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory.setOnClickListener(this);
    }

    private void startCountdown(String endDateStr) {// Start the countdown timer
        SimpleDateFormat dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        try {
            Date endDate = dateFormat.parse(endDateStr);
            if (endDate != null) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                long endTime = endDate.getTime()+day;// Add day to include the current day
                long timeLeft = endTime - currentTime;
                if (timeLeft > 0) {
                    countDownTimer = new CountDownTimer(timeLeft, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            long daysLeft = millisUntilFinished / day;
                            long hoursLeft = (millisUntilFinished / hour) % 24;
                            long minutesLeft = (millisUntilFinished / minute) % 60;
                            long secondsLeft = (millisUntilFinished / second) % 60;

                            tvTimer.setText(String.format(Locale.US, "%d days, %02d:%02d:%02d", daysLeft, hoursLeft, minutesLeft, secondsLeft));
                        }

                        @Override
                        public void onFinish() {
                            tvTimer.setText("Auction Ended!");
                            disableBidding();
                            String currentUid = mAuth.getUid();
                            if (currentUid != null && (currentUid.equals(bid.getItem().getSellerUID()) || currentUid.equals(bid.getLeadingBidderId()))) {
                                fetchAndShowSellerPhone();
                            }
                        }
                    }.start();
                } else {
                    tvTimer.setText("Auction Ended!");
                    disableBidding();

                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            tvTimer.setText("Error loading time");
        }
    }

    private void disableBidding() {//for end of time ,buy now and the creator of the bid
        btnBid.setEnabled(false);
        btnAuto.setEnabled(false);
        btnBuy.setEnabled(false);
        etBidAmount.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null)
            countDownTimer.cancel();
    }

    public void onClick(View v) {
        // Check if bid amount is valid

        if (v == btnBid) {
            if (isExpired) return;
            // Check if bid is valid
            String amount = etBidAmount.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            float bidAmount = Float.parseFloat(amount);
            if (bidAmount <= bid.getHighestOfferedBid()) {
                Toast.makeText(this, "Bid must be higher than current price", Toast.LENGTH_SHORT).show();
                return;
            }
            Request request = new Request(Action.PLACE_BID);
            request.putData("saleId", saleId);
            request.putData("saleCategory", saleCategory);
            request.putData("uid", mAuth.getUid());
            request.putData("bidAmount", bidAmount);
            ServerConnection.getInstance().sendRequest(request, new ServerCallback() {
                @Override
                public void onResponseReceived(Response response) {

                    runOnUiThread(() -> {
                        if (response != null && response.isSuccess()) {
                            Toast.makeText(DisplayBidActivity.this, "Bid Placed Successfully!", Toast.LENGTH_SHORT).show();
                            tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", bidAmount));
                            bid.setHighestOfferedBid(bidAmount);
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Unknown error";
                            Toast.makeText(DisplayBidActivity.this, "Failed to place bid: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else if (v == btnAuto) {
            if (isExpired) return;
            String amount = etBidAmount.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Please enter your maximum limit for AutoBid", Toast.LENGTH_SHORT).show();
                return;
            }
            float maxAutoLimit = Float.parseFloat(amount);
            if (maxAutoLimit <= bid.getHighestOfferedBid()) {
                Toast.makeText(this, "Limit must be higher than current price", Toast.LENGTH_SHORT).show();
                return;
            }

            Request request = new Request(Action.AUTO_BID);
            request.putData("saleId", saleId);
            request.putData("saleCategory", saleCategory);
            request.putData("uid", mAuth.getUid());
            request.putData("maxAutoLimit", maxAutoLimit);
            ServerConnection.getInstance().sendRequest(request, new ServerCallback() {
                @Override
                public void onResponseReceived(Response response) {
                    runOnUiThread(() -> {
                        if (response != null && response.isSuccess()) {
                            Toast.makeText(DisplayBidActivity.this, "AutoBid Activated Successfully!", Toast.LENGTH_SHORT).show();
                            btnAuto.setText("AutoBid Active");
                            btnAuto.setEnabled(false);
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Unknown error";
                            Toast.makeText(DisplayBidActivity.this, "Failed to place Autobid: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });





        } else if (v == btnBuy) {
            if (isExpired) return;
            float buyNowPrice = bid.getMaximumPrice();

            Request request = new Request(Action.BUY_NOW);
            request.putData("saleId", saleId);
            request.putData("saleCategory", saleCategory);
            request.putData("uid", mAuth.getUid());
            request.putData("buyNowPrice", buyNowPrice);

            ServerConnection.getInstance().sendRequest(request, new ServerCallback() {
                @Override
                public void onResponseReceived(Response response) {
                    runOnUiThread(() -> {
                        if (response != null && response.isSuccess()) {
                            Toast.makeText(DisplayBidActivity.this, "Purchased Successfully!", Toast.LENGTH_SHORT).show();
                            tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", buyNowPrice));
                            tvTimer.setText("Auction Closed - Item Purchased!");
                            disableBidding(); //disable bidding buttons
                            fetchAndShowSellerPhone(); //show seller phone

                        } else { // Handle error response
                            String errorMsg = (response != null) ? response.getMessage() : "Unknown error";
                            Toast.makeText(DisplayBidActivity.this, "Failed to Buy Now: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

    } else if (v == ibHomeButton) {
        Intent i = new Intent(this, MainPage.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    } else if (v == ibNotifications) {
        Intent i = new Intent(this, NotificationsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    } else if (v == ibBiddingHistory) {
        Intent i = new Intent(this, BidsHistoryActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }
    }


    private void fetchAndShowSellerPhone() {//get seller phone number to contact seller
        String sellerUid = bid.getItem().getSellerUID();


        Request request = new Request(Action.GET_USER_PHONE);
        request.putData("targetUid", sellerUid);

        ServerConnection.getInstance().sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(() -> {
                    if (response != null && response.isSuccess()) {
                        String phone = (String) response.getData("phone");
                        if (phone != null && !phone.isEmpty()) {
                            tvSellerPhone.setText("Contact Seller: " + phone);
                            tvSellerPhone.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(DisplayBidActivity.this, "Could not fetch seller contact info", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


}