package com.example.tobid.Adapters;

import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.R;

import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;

import java.util.ArrayList;

/**
 * Adapter class for managing and displaying bid data in a RecyclerView.
 */
public class BidAdapter extends RecyclerView.Adapter<BidAdapter.BidViewHolder> {

    // List of bids to be displayed
    private ArrayList<Bid> bids;
    // Click listener for handling item click events
    private View.OnClickListener mOnClickListener;

    public BidAdapter(ArrayList<Bid> bids) {
        this.bids = bids;
    }

    /**
     * Creates and returns a ViewHolder object, inflating the view for a single bid item.
     *
     * @param parent   The parent ViewGroup into which the view will be added.
     * @param viewType The view type of the new View.
     * @return A BidViewHolder instance holding the inflated view.
     */
    @NonNull
    @Override
    public BidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single bid item
        View bidView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycled_bid_item, parent, false);
        return new BidViewHolder(bidView);
    }

    /**
     * Binds data from a Bid object to the corresponding ViewHolder.
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the item in the adapter's data set.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBindViewHolder(@NonNull BidViewHolder holder, int position) {
        // Get the current bid
        Bid bid = bids.get(position);

        Item item = bid.getItem();

        // Set the bid name in the corresponding TextView
        holder.tvBidName.setText(item.getItemName());

        // Fetch and display the bid's current price
        double currentPrice = Math.max(bid.getHighestOfferedBid(), bid.getStartingPrice());
        holder.tvCurrentPrice.setText(String.valueOf(currentPrice));

        // Fetch and display the first (preview) image of the bid
        String imagePath = item.getStoragePathToImg1();
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
                    //byte[] imageBytes = (byte[]) response.getData("imageBytes");
                    String imageUrl = (String) response.getData("imageUrl");
                    // Use Glide to load the image into the ImageView
                    holder.ivImg.post(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(holder.ivImg.getContext())
                                    .load(imageUrl)
                                    .into(holder.ivImg);
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
     * @return The size of the bids list.
     */
    @Override
    public int getItemCount() {
        return bids.size();
    }

    /**
     * ViewHolder class for holding and managing a single bid item view.
     */
    public class BidViewHolder extends RecyclerView.ViewHolder {
        public TextView tvBidName;
        public TextView tvCurrentPrice;
        public ImageView ivImg;

        /**
         * Constructor for BidViewHolder.
         *
         * @param itemView The view representing a single bid item.
         */
        public BidViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize the TextViews and ImageView
            tvBidName = itemView.findViewById(R.id.tvBidName);
            tvCurrentPrice = itemView.findViewById(R.id.tvCurrentPrice);
            ivImg = itemView.findViewById(R.id.ivImg);

            // Set the click listener for the item view
            itemView.setTag(this);
            itemView.setOnClickListener(mOnClickListener);
        }
    }
}
