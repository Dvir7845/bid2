package com.example.tobid.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tobid.Adapters.NotificationAdapter;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

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

        notificationAdapter = new NotificationAdapter(notifications, this);
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

                            notifications.clear();
                            notifications.addAll(newNotifications);
                            notificationAdapter.notifyItemInserted(notifications.size());
                            notificationAdapter.notifyItemRangeChanged(0, notifications.size());

                            System.out.println(notifications);
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
            Intent i = new Intent(this, SalesHistoryActivity.class);
            startActivity(i);
        }

        else if (v == ibHomeButton) {
            Intent i = new Intent(this, MainPage.class);
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

                switch (notificationType) {
                    case SIGNUP:
                        // TODO: send a remove notification request and go to main page
                        break;
                    case BID_CREATED:
                    case LOST_LEAD_IN_BID:
                        // TODO: get specific bid from server and go to bid page
                        break;
                    case BID_WON:
                        // TODO: Go to chat activity with bid creator
                        break;
                }
            }
        };
        notificationAdapter.setmOnClickListener(onItemClickListener);
    }
}