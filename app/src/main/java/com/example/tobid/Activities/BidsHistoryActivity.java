package com.example.tobid.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tobid.Adapters.BidAdapter;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.Request;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

/**
 * Activity for displaying a user's bid history.
 */
public class BidsHistoryActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;
    private TextView tvHostedBidsText, tvParticipatingBidsText;
    private Switch switchDisplayOngoingOrEndedBids;
    private BidAdapter hostedBidsAdapter, participatingBidsAdapter;
    private ArrayList<Bid> hostedBids, participatingBids;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bids_history);

        mAuth = FirebaseAuth.getInstance();

        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        tvHostedBidsText = findViewById(R.id.tvHostedBidsText);
        tvParticipatingBidsText = findViewById(R.id.tvParticipatingBidsText);

        initializeBidDisplayVariables();

        // Display ongoing bids
        displayBids(false);

        switchDisplayOngoingOrEndedBids = findViewById(R.id.switchDisplayOngoingOrEndedBids);
        switchDisplayOngoingOrEndedBids.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Display past hosted and participated bids
                tvHostedBidsText.setText("Previously Hosted Bids:");
                tvParticipatingBidsText.setText("Previously Participated Bids:");
                switchDisplayOngoingOrEndedBids.setText("Displaying past bids");
            } else {
                // Display current hosted and participating bids
                tvHostedBidsText.setText("Hosted Bids:");
                tvParticipatingBidsText.setText("Participating Bids:");
                switchDisplayOngoingOrEndedBids.setText("Displaying ongoing bids");
            }

            displayBids(isChecked);
        });


    }

    private void initializeBidDisplayVariables() {
        hostedBids = new ArrayList<>();
        participatingBids = new ArrayList<>();

        View.OnClickListener hostedBidsOnClickListener = view -> {
            int position = ((RecyclerView.ViewHolder) view.getTag()).getAdapterPosition();
            Bid bid = hostedBids.get(position);
            Intent i = new Intent(BidsHistoryActivity.this, DisplayBidActivity.class);
            i.putExtra("Bid", bid);
            startActivity(i);
        };
        View.OnClickListener participatingBidsOnClickListener = view -> {
            int position = ((RecyclerView.ViewHolder) view.getTag()).getAdapterPosition();
            Bid bid = participatingBids.get(position);
            Intent i = new Intent(BidsHistoryActivity.this, DisplayBidActivity.class);
            i.putExtra("Bid", bid);
            startActivity(i);
        };

        hostedBidsAdapter = new BidAdapter(hostedBids);
        hostedBidsAdapter.setmOnClickListener(hostedBidsOnClickListener);

        participatingBidsAdapter = new BidAdapter(participatingBids);
        participatingBidsAdapter.setmOnClickListener(participatingBidsOnClickListener);

        RecyclerView rvHostedBids = findViewById(R.id.rvHostedBids);
        RecyclerView rvParticipatingBids = findViewById(R.id.rvParticipatingBids);

        rvHostedBids.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHostedBids.setAdapter(hostedBidsAdapter);

        rvParticipatingBids.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvParticipatingBids.setAdapter(participatingBidsAdapter);
    }

    private void displayBids(boolean isChecked) {
        hostedBids.clear();
        participatingBids.clear();

        Request request;
        if (isChecked) {  // Display past hosted and participated bids
            request = new Request(Action.GET_PAST_BIDS);
        } else {  // Display current hosted and participating bids
            // TODO: Implement this (will require adding an active bids per user folder)
           request = new Request(Action.GET_ACTIVE_BIDS);
        }
        request.putData("uid", mAuth.getUid());

        ServerConnection server = ServerConnection.getInstance();
        server.sendRequest(request, response -> runOnUiThread(() -> {
            if (response != null && response.isSuccess()) {
                ArrayList<Bid> receivedBids = (ArrayList<Bid>) response.getData("hostedBids");
                hostedBids.addAll(receivedBids);

                receivedBids = (ArrayList<Bid>) response.getData("participatingBids");
                participatingBids.addAll(receivedBids);

                hostedBidsAdapter.notifyDataSetChanged();
                participatingBidsAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(BidsHistoryActivity.this, "Unable to fetch bids to display from the server", Toast.LENGTH_SHORT).show();
            }
        }));


    }

    @Override
    public void onClick(View v) {
        if (v == ibBiddingHistory) {//only refresh the page
            Intent i = new Intent(this, BidsHistoryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }

        else if (v == ibHomeButton) {
            Intent i = new Intent(this, MainPage.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }

        else if (v == ibNotifications) {
            Intent i = new Intent(this, NotificationsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
    }
}