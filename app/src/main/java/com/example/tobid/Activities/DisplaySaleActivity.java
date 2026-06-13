package com.example.tobid.Activities;

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
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Sale;
import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DisplaySaleActivity extends AppCompatActivity {
    private TextView tvItemName, tvCategory, tvCurrentPrice, tvTimer, tvDetails;
    private EditText etBidAmount;
    private Button btnBid, btnAuto, btnBuy;
    private ImageView imageView1, imageView2, imageView3;
    private Sale sale;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private String saleId, saleCategory;


    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display_sale);
        initViews();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        saleId = getIntent().getStringExtra("saleId");
        saleCategory = getIntent().getStringExtra("saleCategory");
        sale = (Sale) getIntent().getSerializableExtra("Sale");
        mAuth = FirebaseAuth.getInstance();
        if (sale != null) {
            tvItemName.setText(sale.getItem().getItemName());
            tvCategory.setText(sale.getItem().getCategory());
            tvDetails.setText(sale.getItem().getItemDescription());
            tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", sale.getStartingPrice()));

            if (sale.isHasMaximumPrice()) {
                btnBuy.setVisibility(View.VISIBLE);
                btnBuy.setText("BUY IT NOW FOR $" + sale.getMaximumPrice());
            } else {
                btnBuy.setVisibility(View.GONE);
            }
            startCountdown(sale.getEndDate());
        }
        setupClickListeners();

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
// ToDo: need to change to work with server
    private void setupClickListeners() {
        // Set up click listeners for buttons
            btnBid.setOnClickListener(v -> {
                String amount = etBidAmount.getText().toString();
                if (!amount.isEmpty()) {
                    float bidAmount = Float.parseFloat(amount);
                    if (bidAmount <= sale.getHighestOfferedBid()) {
                        Toast.makeText(this, "Bid must be higher than current price", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sale.setHighestOfferedBid(bidAmount);
                    sale.setLeadingBidderId(mAuth.getUid());
                    myRef.setValue(sale);
                    tvCurrentPrice.setText(String.format(Locale.US, "$%.2f", bidAmount));
                    Toast.makeText(this, "Bid Placed: $" + amount, Toast.LENGTH_SHORT).show();
                    //try to send bid to server
                    new Thread(() -> {
                        Request request = new Request(Action.PLACE_BID);
                        request.putData("saleId", saleId);
                        request.putData("uid", mAuth.getUid());
                        request.putData("bidAmount", bidAmount);

//                        Response response = ServerConnection.getInstance().sendRequest(request);
//                        runOnUiThread(() -> {
//                            if (response != null && response.isSuccess()) {
//                                Toast.makeText(DisplaySaleActivity.this, "Server updated successfully!", Toast.LENGTH_SHORT).show();
//                            } else {
//                                String errorMsg = (response != null) ? response.getMessage() : "Unknown error";
//                                Toast.makeText(DisplaySaleActivity.this, "Server update failed: " + errorMsg, Toast.LENGTH_LONG).show();
//                            }
//                        });
                    }).start();
                    //end of send bid to server
                } else {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                }

            });
        btnAuto.setOnClickListener(v -> {
            Toast.makeText(this, "Automatic Bid Activated", Toast.LENGTH_SHORT).show();
        });

        btnBuy.setOnClickListener(v -> {
            Toast.makeText(this, "Item Purchased via Buy It Now!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null)
            countDownTimer.cancel();
    }

    public void onClick(View v) {
        if (v == btnBid) {
            String amount = etBidAmount.getText().toString();
            if (!amount.isEmpty()) {
                Toast.makeText(this, "Bid Placed: $" + amount, Toast.LENGTH_SHORT).show();
                sale.setHighestOfferedBid(Float.parseFloat(amount));
                sale.setLeadingBidderId(mAuth.getUid());
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        } else if (v == btnAuto) {

        } else if (v == btnBuy) {
        }
    }
}