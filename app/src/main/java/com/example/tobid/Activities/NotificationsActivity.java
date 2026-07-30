package com.example.tobid.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tobid.Adapters.NotificationAdapter;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

/**
 * Activity for displaying and managing user notifications.
 */
public class NotificationsActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    // Adapter for managing notifications
    private NotificationAdapter notificationAdapter;
    private ArrayList<Notification> notifications;
    private RecyclerView rvNotifications;
    private View.OnClickListener onItemClickListener;
    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();

        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        // Initialize RecyclerView and notifications list
        notifications = new ArrayList<>();
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        notificationAdapter = new NotificationAdapter(notifications);
        rvNotifications.setAdapter(notificationAdapter);

        // Fetch notifications from the server
        Request request = new Request(Action.GET_USER_NOTIFICATIONS);
        request.putData("uid", mAuth.getUid());

        ServerConnection server = ServerConnection.getInstance();
        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                // When response is received update ui
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            ArrayList<Notification> newNotifications = (ArrayList<Notification>) response.getData("notifications");

                            if (newNotifications != null) {
                                notifications.clear();

                                notifications.addAll(newNotifications);
                                notificationAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Toast.makeText(NotificationsActivity.this,
                                    "Server couldn't fetch notifications.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Set up the notification click listener
        setupNotificationClickListener();
    }

    @Override
    public void onClick(View v) {
        if (v == ibBiddingHistory) {
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
            //Intent i = new Intent(this, NotificationsActivity.class);
            //startActivity(i);
        }
    }

    /**
     * Sets up the onClickListener for notifications to handle various notification types.
     */
    private void setupNotificationClickListener() {
        onItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
                int position = viewHolder.getAdapterPosition();
                Notification notification = notifications.get(position);
                NotificationType notificationType = notification.getNotificationType();

                ServerConnection server = ServerConnection.getInstance();
                Request request;
                // Handle different notification types
                switch (notificationType) {
                    case SIGNUP:  // Send a remove notification request and go to main page
                        break;
                    case BID_CREATED:
                    case LOST_LEAD_IN_BID:
                    case BID_WON:
                        // Get specific bid from server and go to bid page
                        request = new Request(Action.GET_BID_BY_BID_ID);
                        request.putData("bidId", notification.getSenderId());

                        server.sendRequest(request, new ServerCallback() {
                            @Override
                            public void onResponseReceived(Response response) {
                                runOnUiThread(() -> {
                                    if (response != null && response.isSuccess()) {
                                        Bid bid = (Bid) response.getData("Bid");
                                        System.out.println(bid);
                                        Intent i = new Intent(NotificationsActivity.this, DisplayBidActivity.class);
                                        i.putExtra("Bid", bid);
                                        startActivity(i);
                                    } else {
                                        Toast.makeText(NotificationsActivity.this, "Bid retrieval failed.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                        break;
                }

                request = new Request(Action.REMOVE_NOTIFICATION_BY_ID);
                request.putData("uid", mAuth.getUid());
                request.putData("notificationId", notification.getId());

                server.sendRequest(request, new ServerCallback() {
                    @Override
                    public void onResponseReceived(Response response) {
                        if (response != null && response.isSuccess()) {
                            if (notificationType == NotificationType.SIGNUP) {
                                Intent i = new Intent(NotificationsActivity.this, MainPage.class);
                                startActivity(i);
                            }
                        }
                    }
                });

            }
        };
        notificationAdapter.setmOnClickListener(onItemClickListener);
    }
}