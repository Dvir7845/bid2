package com.example.tobid.Adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter class for displaying notifications in a RecyclerView.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    // List of Notifications to be displayed
    private ArrayList<Notification> notifications;
    private View.OnClickListener mOnClickListener;
    // Context for resource access
    private Context context;

    public NotificationAdapter(ArrayList<Notification> notifications, Context context) {
        this.notifications = notifications;
        this.context = context;
    }

    /**
     * Creates and returns a ViewHolder object, inflating the view for a single notification item.
     *
     * @param parent   The parent ViewGroup into which the view will be added.
     * @param viewType The view type of the new View.
     * @return A NotificationViewHolder instance holding the inflated view.
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single notification item
        View notificationView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycled_notification_item, parent, false);
        return new NotificationViewHolder(notificationView);
    }

    /**
     * Binds data from a Notification object to the corresponding ViewHolder.
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the item in the adapter's data set.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        // Get the current notification
        Notification notification = notifications.get(position);

        // Set the notification text
        holder.tvNotificationText.setText(notification.getMessage());

        // Get the sender's image from the server
        String imagePath = notification.getSenderImg();
        if (imagePath == null) {
            Log.e("BidAdapter", "Bid image is null.");
            return;
        }

        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_IMAGE_BY_PATH);
        request.putData("imagePath", imagePath);

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                if (response != null && response.isSuccess()) {
                    String imageUrl = (String) response.getData("imageUrl");
                    // Use Glide to load the image into the ImageView
                    holder.ivPfp.post(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(holder.ivPfp.getContext())
                                    .load(imageUrl)
                                    .into(holder.ivPfp);
                        }
                    });
                }
            }
        });
    }

    /**
     * Sets a click listener for the RecyclerView items.
     *
     * @param mOnClickListener The click listener to set.
     */
    public void setmOnClickListener(View.OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    /**
     * Returns the total number of items in the adapter.
     *
     * @return The size of the notifications list.
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder class for holding and managing a single notification item view.
     */
    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        // TextView for displaying the notification message
        public TextView tvNotificationText;
        // ImageView for displaying the sender's profile picture
        public CircleImageView ivPfp;

        /**
         * Constructor for NotificationViewHolder.
         *
         * @param itemView The view representing a single notification item.
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize the TextView and ImageView
            tvNotificationText = itemView.findViewById(R.id.tvNotificationText);
            ivPfp = itemView.findViewById(R.id.ivPfp);

            // Set the click listener for the item view
            itemView.setTag(this);
            itemView.setOnClickListener(mOnClickListener);
        }
    }
}
