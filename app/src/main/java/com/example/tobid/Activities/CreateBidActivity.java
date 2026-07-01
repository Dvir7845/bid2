package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class CreateBidActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGES_REQUEST = 1;
    private ArrayList<Uri> imageUris = new ArrayList<>();
    private FirebaseAuth mAuth;

    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;

    private Calendar bidStartDate, bidEndDate;
    private LinearLayout layoutMaximumPriceSection;
    private TextView tvBidStartDate, tvBidEndDate;
    private EditText etItemName, etItemDetails, etStartingPrice, etMaximumPrice;
    private Switch swIsMaximumPrice;
    private Spinner spCategory;
    private Button btnSetStartDate, btnSetEndDate, btnSelectImages, btnCreateBidding;

    private final int maximumImageAmount = 3;
    private ImageView ivImg1, ivImg2, ivImg3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_bid);

        // Initialize auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize navigation bar buttons
        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        // Initialize bidding creation Views
        etItemName = findViewById(R.id.etItemName);
        etItemDetails = findViewById(R.id.etItemDetails);

        etStartingPrice = findViewById(R.id.etStartingPrice);
        etMaximumPrice = findViewById(R.id.etMaximumPrice);
        layoutMaximumPriceSection = findViewById(R.id.layoutMaximumPriceSection);
        layoutMaximumPriceSection.setVisibility(View.GONE);
        swIsMaximumPrice = findViewById(R.id.swIsMaximumPrice);
        swIsMaximumPrice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    layoutMaximumPriceSection.setVisibility(View.VISIBLE);
                } else {
                    layoutMaximumPriceSection.setVisibility(View.GONE);
                }
            }
        });

        tvBidStartDate = findViewById(R.id.tvBidStartDate);
        tvBidEndDate = findViewById(R.id.tvBidEndDate);

        ivImg1 = findViewById(R.id.ivImg1);
        ivImg2 = findViewById(R.id.ivImg2);
        ivImg3 = findViewById(R.id.ivImg3);

        btnSetStartDate = findViewById(R.id.btnSetStartDate);
        btnSetStartDate.setOnClickListener(this);

        btnSetEndDate = findViewById(R.id.btnSetEndDate);
        btnSetEndDate.setOnClickListener(this);

        btnSelectImages = findViewById(R.id.btnSelectImages);
        btnSelectImages.setOnClickListener(this);

        btnCreateBidding = findViewById(R.id.btnCreateBidding);
        btnCreateBidding.setOnClickListener(this);

        // Get categories from db and display in spinner
        spCategory = findViewById(R.id.spCategory);

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

                            ArrayAdapter<String> spinnerAdapter =
                                    new ArrayAdapter<>(CreateBidActivity.this, android.R.layout.simple_spinner_item, categories);
                            spinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
                            spCategory.setAdapter(spinnerAdapter);
                        } else {
                            Toast.makeText(CreateBidActivity.this, "Category fetch failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == ibBiddingHistory) {
            Intent i = new Intent(this, BidsHistoryActivity.class);
            startActivity(i);
        }

        else if (v == ibHomeButton) {
            Intent i = new Intent(this, MainPage.class);
            startActivity(i);
        }

        else if (v == ibNotifications) {
            Intent i = new Intent(this, NotificationsActivity.class);
            startActivity(i);
        }
        else if (v == btnSelectImages) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGES_REQUEST);
        }
        else if (v == btnSetStartDate) {
            selectStartDateAndDisplayIn(tvBidStartDate);

        }
        else if (v == btnSetEndDate) {
            selectEndDateAndDisplayIn(tvBidEndDate);
        }
        else if (v == btnCreateBidding) {
            String bidId = mAuth.getUid() + Calendar.getInstance().getTimeInMillis();

            // Get Data
            String itemName = etItemName.getText().toString();
            Object selectedCategoryObj = spCategory.getSelectedItem();
            // Convert to string and check for empty
            String itemCategory = (selectedCategoryObj != null) ? selectedCategoryObj.toString() : "";
            String startingPriceStr = etStartingPrice.getText().toString();
            boolean isMaximumPrice = swIsMaximumPrice.isChecked();
            String maximumPriceStr = etMaximumPrice.getText().toString();

            String bidStartDateFormatted = tvBidStartDate.getText().toString();
            String bidEndDateFormatted = tvBidEndDate.getText().toString();
            String itemDetails = etItemDetails.getText().toString();

            // Get chosen images for the bidding
            String[] imagePaths = new String[maximumImageAmount];
            for (int i = 0; i < imageUris.size(); i++) {
                if (imageUris.get(i) != null) {
                    // Upload image to Firebase Storage
                    String imagePath = "Bids/" + itemCategory + "/" + bidId + "/image" + (i+1);
                    imagePaths[i] = imagePath;
                }
            }

            // Test validity
            boolean isValid = true;

            if (itemName.isEmpty()) {
                Toast.makeText(CreateBidActivity.this, "Please enter a name for the item", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (itemCategory.isEmpty()) {
                Toast.makeText(CreateBidActivity.this, "Please select an item category", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (startingPriceStr.isEmpty()) {
                Toast.makeText(CreateBidActivity.this, "Please enter a starting price", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (isMaximumPrice && maximumPriceStr.isEmpty()) {
                Toast.makeText(CreateBidActivity.this, "Please enter a maximum price", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            else if (bidStartDate == null) {
                Toast.makeText(CreateBidActivity.this, "Please select a starting date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (bidEndDate == null) {
                Toast.makeText(CreateBidActivity.this, "Please select a end date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (bidStartDate.after(bidEndDate) || bidStartDate.equals(bidEndDate)) {
                Toast.makeText(CreateBidActivity.this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (imagePaths[0] == null) { // Ensure at least one image is selected
                Toast.makeText(CreateBidActivity.this, "Please select at least one image", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            if (!isValid) {
                return;
            }
            float startingPrice = Float.parseFloat(startingPriceStr);
            float maximumPrice = isMaximumPrice ? Float.parseFloat(maximumPriceStr) : -1;



            // Send a CREATE_SALE Request
            String itemId = itemName + Calendar.getInstance().getTimeInMillis();
            Item item = new Item(itemName, itemId, itemDetails, itemCategory, mAuth.getUid(), imagePaths[0], imagePaths[1], imagePaths[2]);
            Bid bid = new Bid(item, bidStartDateFormatted, bidEndDateFormatted, startingPrice, isMaximumPrice, maximumPrice);

            Request request = new Request(Action.CREATE_BID);
            request.putData("bidId", bidId);
            request.putData("Bid", bid);
            request.putData("imagePaths", imagePaths);

            for (int i=0; i< imageUris.size(); i++) {
                Uri imageUri = imageUris.get(i);
                if (imageUri == null) continue;

                byte[] imageBytes = uriToBytes(imageUri);
                request.putFile("Image" + (i+1), imageBytes);
            }

            // Send request to server
            ServerConnection server = ServerConnection.getInstance();
            server.sendRequest(request, new ServerCallback() {
                @Override
                public void onResponseReceived(Response response) {
                    // When response is received update ui
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null && response.isSuccess()) {
                                // Go to bid detail page
                                Intent i = new Intent(CreateBidActivity.this, DisplayBidActivity.class);
                                i.putExtra("Bid", bid);
                                startActivity(i);
                            }
                            else {
                                Toast.makeText(CreateBidActivity.this, "Server side error, couldn't create bid. Please try again later", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }
    }

    private void selectStartDateAndDisplayIn(TextView tvBidStartDate) {
        DatePickerDialog bidStartDatePicker = new DatePickerDialog(CreateBidActivity.this);

        // Don't let the user select past dates
        bidStartDatePicker.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());

        bidStartDatePicker.setOnDateSetListener((datePicker, year, month, dayOfMonth) -> {
            bidStartDate = Calendar.getInstance();
            bidStartDate.set(year, month, dayOfMonth);

            String formattedDate = String.format("%02d-%02d-%d", dayOfMonth, (month + 1), year);
            tvBidStartDate.setText(formattedDate);
        });
        bidStartDatePicker.show();
    }

    private void selectEndDateAndDisplayIn(TextView tvBidEndDate) {
        DatePickerDialog bidEndDatePicker = new DatePickerDialog(CreateBidActivity.this);

        bidEndDatePicker.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
        if (bidStartDate != null) {
            bidEndDatePicker.getDatePicker().setMinDate(bidStartDate.getTimeInMillis());
        }

        bidEndDatePicker.setOnDateSetListener((datePicker, year, month, dayOfMonth) -> {
            bidEndDate = Calendar.getInstance();
            bidEndDate.set(year, month, dayOfMonth);

            String formattedDate = String.format("%02d-%02d-%d", dayOfMonth, (month + 1), year);
            tvBidEndDate.setText(formattedDate);
        });
        bidEndDatePicker.show();
    }

    // Handling Activity result for image selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {


            if (data.getClipData() != null) { // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count && i < maximumImageAmount; i++) {
                    if (imageUris.size() <= maximumImageAmount) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    } else {
                        Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            } else if (data.getData() != null) {
                    if (imageUris.size() < maximumImageAmount) {
                        imageUris.add(data.getData());
                    } else {
                        Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
                    }

            }
            displayImages();
        }
    }

    private void displayImages() {
        if (imageUris.size() > 0) ivImg1.setImageURI(imageUris.get(0));
        if (imageUris.size() > 1) ivImg2.setImageURI(imageUris.get(1));
        if (imageUris.size() > 2) ivImg3.setImageURI(imageUris.get(2));
    }

    public byte[] uriToBytes(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            // Read the image data in 8KB chunks
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            // Return image data byteArray
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
