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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tobid.DataModels.Action;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.User;
import com.example.tobid.R;
import com.example.tobid.ServerCommunicationClasses.ServerCallback;
import com.example.tobid.ServerCommunicationClasses.ServerConnection;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class ChangeProfilePage extends AppCompatActivity implements View.OnClickListener {

    // Firebase auth
    private FirebaseAuth mAuth;

    private String uid;
    private User userData;

    private String fetchedUsername;

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

        // Initialize Auth
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

        // Initialize username and profile image
        etChangeUsername = findViewById(R.id.etChangeUsername);
        ivPfp = findViewById(R.id.ivPfp);

        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_USER_BY_ID);
        request.putData("uid", mAuth.getUid());

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            userData = (User) response.getData("user");
                            String username = userData.getUsername();
                            fetchedUsername = username; // Used to check if username has been changed and we need to update it
                            etChangeUsername.setText(username);

                            String imagePath = userData.getImg();
                            fetchAndDisplayImage(imagePath);
                        } else {
                            Toast.makeText(ChangeProfilePage.this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Initialize buttons
        btnChangeProfile = findViewById(R.id.btnChangeProfile);
        btnChangeProfile.setOnClickListener(this);

        // Initialize the change image button
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnChangeImage.setOnClickListener(this);

        // Initialize logout button
        btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(this);
    }

    private void fetchAndDisplayImage(String imagePath) {
        ServerConnection server = ServerConnection.getInstance();
        Request request = new Request(Action.GET_IMAGE_BY_PATH);
        request.putData("imagePath", imagePath);

        server.sendRequest(request, new ServerCallback() {
            @Override
            public void onResponseReceived(Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null && response.isSuccess()) {
                            String imageUrl = (String) response.getData("imageUrl");
                            System.out.println(imageUrl);
                            // Use Glide to load the image into the ImageView
                            ivPfp.post(new Runnable() {
                                @Override
                                public void run() {
                                    Glide.with(ivPfp.getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_pfp)
                                            .into(ivPfp);
                                }
                            });
                        } else {
                            Toast.makeText(ChangeProfilePage.this, "Failed to get profile picture", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
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

            // Check if data needs updating
            String newUsername = etChangeUsername.getText().toString();
            boolean doesUsernameNeedUpdating = !fetchedUsername.equals(newUsername);
            boolean doesPictureNeedUpdating = selectedImageUri != null || photo != null;
            System.out.println(fetchedUsername + " " + newUsername);

            if (!doesUsernameNeedUpdating && !doesPictureNeedUpdating) return;

            // Else, data needs updating
            ServerConnection server = ServerConnection.getInstance();
            Request request = new Request(Action.CHANGE_USERNAME_AND_PICTURE);
            request.putData("uid", uid);
            request.putData("doesUsernameNeedUpdating", doesUsernameNeedUpdating);
            request.putData("doesPictureNeedUpdating", doesPictureNeedUpdating);

            // Add username data to request
            request.putData("newUsername", newUsername);

            // Add image data to request
            String imagePath = "Users/" + uid + "/profilePicture-" + Calendar.getInstance().getTimeInMillis() + ".png";
            request.putData("imagePath", imagePath);
            request.putData("currentImagePath", userData.getImg());
            byte[] imageBytes;
            if (selectedImageUri != null) {
                imageBytes = uriToBytes(selectedImageUri);
            } else if (photo != null) {
                imageBytes = bitmapToBytes(photo);
            } else {
                imageBytes = null;
            }
            request.putFile("image", imageBytes);

            server.sendRequest(request, new ServerCallback() {
                @Override
                public void onResponseReceived(Response response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null && response.isSuccess()) {
                                if (doesPictureNeedUpdating) {
                                    Glide.with(ivPfp.getContext())
                                            .load(imageBytes)
                                            .placeholder(R.drawable.default_pfp)
                                            .into(ivPfp);
                                }

                                Toast.makeText(ChangeProfilePage.this, "Data updated.", Toast.LENGTH_SHORT).show();

                                // Navigate back to the main page
                                Intent i = new Intent(ChangeProfilePage.this, MainPage.class);
                                startActivity(i);
                            } else {
                                Toast.makeText(ChangeProfilePage.this, "Failed updating data.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
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

    private byte[] bitmapToBytes(Bitmap photo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bitmapPhotoBytes = baos.toByteArray();

        return bitmapPhotoBytes;
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
