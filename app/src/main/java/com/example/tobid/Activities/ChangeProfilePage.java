package com.example.tobid.Activities;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import com.example.tobid.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

public class ChangeProfilePage extends AppCompatActivity implements View.OnClickListener {

    // Firebase components
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private String uid;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // UI components
    private de.hdodenhof.circleimageview.CircleImageView ivPfp;
    private Button btnChangeImage, btnChangeProfile, btnLogOut;
    private ImageButton ibHomeButton, ibNotifications, ibBiddingHistory;
    private EditText etChangeUsername;

    // Image selection
    private Uri selectedImageUri;
    private Bitmap photo;

    @Override
    public void onStart() {
        super.onStart();
        // Reload user information if signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            currentUser.reload();
        }
    }

    private final int SELECT_PICTURE = 200;  // Image selector constant
    private final int CAMERA_REQUEST = 1888;  // Camera request constant

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_profile_page);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        uid = user.getUid();

        // Initialize buttons and set click listeners
        ibHomeButton = findViewById(R.id.ibHomeButton);
        ibHomeButton.setOnClickListener(this);
        ibNotifications = findViewById(R.id.ibNotifications);
        ibNotifications.setOnClickListener(this);
        ibBiddingHistory = findViewById(R.id.ibBiddingHistory);
        ibBiddingHistory.setOnClickListener(this);

        // Initialize username input
        etChangeUsername = findViewById(R.id.etChangeUsername);

        // Retrieve current username from Firebase and set it in the EditText
        myRef = database.getReference("/Users/" + uid + "/username");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Fetch the username from the database and set it in the input field
                String value = dataSnapshot.getValue(String.class);
                etChangeUsername.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read username.", error.toException());
            }
        });

        // Initialize buttons
        btnChangeProfile = findViewById(R.id.btnChangeProfile);
        btnChangeProfile.setOnClickListener(this);

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();

        // Display the profile picture
        ivPfp = findViewById(R.id.ivPfp);
        database.getReference("/Users/" + uid + "/img").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentImagePath = snapshot.getValue(String.class);
                storageRef = storage.getReference(currentImagePath);
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Load the profile picture from Firebase Storage into the ImageView using Glide
                    Glide.with(ChangeProfilePage.this)
                            .load(uri)
                            .into(ivPfp);
                }).addOnFailureListener(exception -> {
                    Log.e("FirebaseStorage", "Failed to get download URL: " + exception.getMessage());
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Initialize the change image button
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnChangeImage.setOnClickListener(this);

        // Initialize logout button
        btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(this);
    }

    // Function to launch an image chooser (Gallery)
    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // Function to launch the camera for taking a picture
    void cameraPicture() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    // Function to handle the result after selecting or capturing an image
    public void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                selectedImageUri = intentData.getData();
                if (selectedImageUri != null) {
                    ivPfp.setImageURI(selectedImageUri);
                }
            }

            if (requestCode == CAMERA_REQUEST) {
                photo = (Bitmap) intentData.getExtras().get("data");
                ivPfp.setImageBitmap(photo);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnChangeProfile) {
            // Update username in Firebase Database
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                return;
            }
            myRef = database.getReference("/Users/" + uid + "/username");
            myRef.setValue(etChangeUsername.getText().toString());

            // If profile picture needs to be updated
            if (selectedImageUri != null || photo != null) {
                // Delete previous profile picture
                database.getReference("/Users/" + uid + "/img").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String currentImagePath = snapshot.getValue(String.class);
                        storage.getReference(currentImagePath).delete();
                        // Upload the selected profile picture to Firebase Storage
                        String imagePath = "/Users/" + uid + "/profilePicture-" + Calendar.getInstance().getTimeInMillis() + ".png";
                        storageRef = storage.getReference(imagePath);

                        // User chose gallery image
                        if (selectedImageUri != null) {
                            UploadTask uploadTask = storageRef.putFile(selectedImageUri);
                            uploadTask.addOnFailureListener(exception -> {
                                // Handle failure during upload
                            }).addOnSuccessListener(taskSnapshot -> {
                                // Once the image upload is successful, update the user's profile
                                myRef = database.getReference("/Users/" + uid + "/img");
                                myRef.setValue(imagePath);
                            });
                        }
                        // User chose camera photo
                        else if (photo != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] data = baos.toByteArray();

                            UploadTask uploadTask = storageRef.putBytes(data);
                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Once the image upload is successful, update the user's profile
                                    myRef = database.getReference("/Users/" + uid + "/img");
                                    myRef.setValue(imagePath);
                                }
                            });
                        }

                        // Navigate back to the main page
                        Intent i = new Intent(ChangeProfilePage.this, MainPage.class);
                        startActivity(i);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            // Navigate back to the main page
            Intent i = new Intent(ChangeProfilePage.this, MainPage.class);
            startActivity(i);

        }
        else if (v == btnChangeImage) {
            // Show a dialog allowing the user to choose between the gallery or camera
            AlertDialog.Builder cameraOrGalleryDialog = new AlertDialog.Builder(ChangeProfilePage.this);
            cameraOrGalleryDialog.setMessage("Take image from");
            cameraOrGalleryDialog.setPositiveButton("Gallery", (dialog, which) -> imageChooser());
            cameraOrGalleryDialog.setNegativeButton("Camera", (dialog, which) -> cameraPicture());
            cameraOrGalleryDialog.create().show();
        }

        else if (v == btnLogOut) {
            // Log out the user and redirect to the SignInPage
            mAuth.signOut();
            Intent i = new Intent(this, SignInActivity.class);
            startActivity(i);
        }

        else if (v == ibBiddingHistory) {
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
    }
}
