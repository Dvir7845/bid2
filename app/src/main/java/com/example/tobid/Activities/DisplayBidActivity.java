package com.example.tobid.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DisplayBidActivity extends AppCompatActivity implements View.OnClickListener{
    private TextView tvItemName, tvCategory, tvCurrentPrice, tvTimer, tvDetails;
    private EditText etBidAmount;
    private Button btnBid, btnAuto, btnBuy;
    private ImageView imageView1, imageView2, imageView3;
    private Bid bid;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private String saleId, saleCategory;


    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display_bid);
        initViews();

        // Initialize Firebase components
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
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

        saleId = item.getItemId();
        saleCategory = item.getCategory();

        tvItemName.setText(bid.getItem().getItemName());
        tvCategory.setText(bid.getItem().getCategory());
        tvDetails.setText(bid.getItem().getItemDescription());
        tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", bid.getStartingPrice()));

        if (bid.isHasMaximumPrice()) {
            btnBuy.setVisibility(View.VISIBLE);
            btnBuy.setText("BUY IT NOW FOR $" + bid.getMaximumPrice());
        } else {
            btnBuy.setVisibility(View.GONE);
        }

        // If the user is the creator of the bid
        if(mAuth.getUid().equals(bid.getItem().getSellerUID())){
            btnBid.setEnabled(false);
            btnAuto.setEnabled(false);
        }

        startCountdown(bid.getEndDate());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        tvTimer = findViewById(R.id.tvTimer);
        tvDetails = findViewById(R.id.tvDetails);
        etBidAmount = findViewById(R.id.etBidAmount);
        btnBid = findViewById(R.id.btnBid);
        btnAuto = findViewById(R.id.btnAuto);
        btnBuy = findViewById(R.id.btnBuy);
        imageView1 = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        imageView3 = findViewById(R.id.imageView3);
    }

    private void startCountdown(String endDateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        try {
            Date endDate = dateFormat.parse(endDateStr);
            if (endDate != null) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                long endTime = endDate.getTime();
                long timeLeft = endTime - currentTime;
                if (timeLeft > 0) {
                    countDownTimer = new CountDownTimer(timeLeft, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            long days = millisUntilFinished / (1000 * 60 * 60 * 24);
                            long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                            long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                            long seconds = (millisUntilFinished / 1000) % 60;

                            tvTimer.setText(String.format(Locale.US, "%d days, %02d:%02d:%02d", days, hours, minutes, seconds));
                        }

                        @Override
                        public void onFinish() {
                            tvTimer.setText("Auction Ended!");
                            disableBidding();
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

    private void disableBidding() {
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
        if (v == btnBid) {
            // Check if bid is valid
            String amount = etBidAmount.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            float bidAmount = Float.parseFloat(amount);
            //check before request to server to see if bid is valid
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
                            bid.setHighestOfferedBid(bidAmount); // עדכון האובייקט המקומי
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Unknown error";
                            Toast.makeText(DisplayBidActivity.this, "Failed to place bid: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else if (v == btnAuto) {
            // TODO: Make auto bid feature in server
            Toast.makeText(this, "Automatic Bid Activated", Toast.LENGTH_SHORT).show();
        } else if (v == btnBuy) {
            // TODO: Make buy button feature in server
            Toast.makeText(this, "Item Purchased via Buy It Now!", Toast.LENGTH_SHORT).show();
        }
    }
}