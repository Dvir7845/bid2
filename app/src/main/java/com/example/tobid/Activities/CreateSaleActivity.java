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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tobid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class CreateSaleActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGES_REQUEST = 1;
    private ArrayList<Uri> imageUris = new ArrayList<>();

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;

    private Calendar bidStartDate, bidEndDate;
    private TextView tvBidStartDate, tvBidEndDate;
    private EditText etItemName, etItemDetails;
    private Spinner spCategory;
    private Button btnSetStartDate, btnSetEndDate, btnSelectImages, btnCreateBidding;

    private final int maximumImageAmount = 3;
    private ImageView ivImg1, ivImg2, ivImg3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_sale);

        // Initialize firebase
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

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

        myRef.child("Categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<ArrayList<String>> type = new GenericTypeIndicator<ArrayList<String>>() {};
                ArrayList<String> categories = snapshot.getValue(type);

                ArrayAdapter<String> spinnerAdapter =
                        new ArrayAdapter<>(CreateSaleActivity.this, android.R.layout.simple_spinner_item, categories);
                spinnerAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
                spCategory.setAdapter(spinnerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read categories", error.toException());
            }
        });


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
            String itemCategory = spCategory.getSelectedItem().toString();
            String bidStartDateFormatted = tvBidStartDate.getText().toString();
            String bidEndDateFormatted = tvBidEndDate.getText().toString();
            String itemDetails = etItemDetails.getText().toString();

            // Get chosen images for the bidding
            String[] imgPaths = new String[maximumImageAmount];
            for (int i = 0; i < imageUris.size(); i++) {
                if (imageUris.get(i) != null) {
                    // Upload image to Firebase Storage
                    String imagePath = "/Bids/" + bidId + "/image" + (i+1);
                    imgPaths[i] = imagePath;
                }
            }

            // Test validity
            boolean isValid = true;

            if (itemName.isEmpty()) {
                Toast.makeText(CreateSaleActivity.this, "Please enter a name for the item", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (itemCategory.isEmpty()) {
                Toast.makeText(CreateSaleActivity.this, "Please select an item category", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (bidStartDate == null) {
                Toast.makeText(CreateSaleActivity.this, "Please select a starting date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (bidEndDate == null) {
                Toast.makeText(CreateSaleActivity.this, "Please select a end date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (bidStartDate.after(bidEndDate)) {
                Toast.makeText(CreateSaleActivity.this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (imgPaths[0] == null) { // Ensure at least one image is selected
                Toast.makeText(CreateSaleActivity.this, "Please select at least one image", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            // Upload Item images to storage
            for (int i = 0; i < imageUris.size(); i++) {
                if (imageUris.get(i) != null) {
                    // Upload image to Firebase Storage
                    String imagePath = imgPaths[i];
                    storageRef = storage.getReference(imagePath);
                    storageRef.putFile(imageUris.get(i));
                    imgPaths[i] = imagePath;
                }
            }

            String itemId = itemName + Calendar.getInstance().getTimeInMillis();
            // Create Bid and Sale then upload to db
            //Item item = new Item(itemId, imgPaths[0], imgPaths[1], imgPaths[2], itemDetails, );



        }
    }

    private void selectStartDateAndDisplayIn(TextView tvBidStartDate) {
        DatePickerDialog bidEndDate = new DatePickerDialog(CreateSaleActivity.this);

        // Don't let the user select past dates
        bidEndDate.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());

        bidEndDate.setOnDateSetListener((datePicker, year, month, dayOfMonth) ->

                tvBidStartDate.setText(dayOfMonth + "-" + (month + 1) + "-" + year));
        bidEndDate.show();
    }

    private void selectEndDateAndDisplayIn(TextView tvBidEndDate) {
        DatePickerDialog bidStartdDate = new DatePickerDialog(CreateSaleActivity.this);

        if (bidStartDate != null) {
            bidStartdDate.getDatePicker().setMinDate(bidStartDate.getTimeInMillis());
        }

        bidStartdDate.setOnDateSetListener((datePicker, year, month, dayOfMonth) ->

                tvBidStartDate.setText(dayOfMonth + "-" + (month + 1) + "-" + year));
        bidStartdDate.show();
    }

    // Handling Activity result for image selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUris.clear();
            ivImg1.setImageURI(null);
            ivImg2.setImageURI(null);
            ivImg3.setImageURI(null);

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count && i < maximumImageAmount; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }
            displayImages();
        }
    }

    private void displayImages() {
        if (imageUris.size() > 0) ivImg1.setImageURI(imageUris.get(0));
        if (imageUris.size() > 1) ivImg2.setImageURI(imageUris.get(1));
        if (imageUris.size() > 2) ivImg3.setImageURI(imageUris.get(2));
    }
    private void selectDateAndDisplayIn(TextView tvDateText, boolean isStartDate) {
        DatePickerDialog bidStartOrEndDate = new DatePickerDialog(CreateSaleActivity.this);

        // Don't let the user select past dates
        bidStartOrEndDate.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());


    }
}
