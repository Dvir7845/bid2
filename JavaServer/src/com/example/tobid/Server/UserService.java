package com.example.tobid.Server;

import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.User;
import com.example.tobid.DataModels.FirebaseStorageService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class UserService {
    private static UserService instance;
    private final FirebaseDatabase database;

    private UserService() {
        this.database = FirebaseDatabase.getInstance();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    protected Response handleGetUserNotifications(Request request) {
    	final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    	
    	ArrayList<Notification> notifications = new ArrayList<>();
		try {
			String uid = (String) request.getData("uid");
			DatabaseReference myRef = database.getReference().child("Users").child(uid).child("notifications");

	        myRef.addListenerForSingleValueEvent(new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot snapshot) {
					System.out.println("fetched notifications");
					for (DataSnapshot datas : snapshot.getChildren()) {
	                    Notification notification = datas.getValue(Notification.class);
	                    notifications.add(notification);
	                }
					System.out.println("got notifications");
					latch.countDown();
				}

				@Override
				public void onCancelled(DatabaseError error) {
					System.out.println("Error fetching requests for user: " + uid);
					latch.countDown();
				}
	        });
			
	        // Wait for the db to fetch the notifications
	        System.out.println("waiting for latch");
	        latch.await();
	        System.out.println("latch received");
	        Response response = new Response(true, "Notifications fetch successfuly");
	        response.putData("notifications", notifications);
	        return response; 
			
		} catch (Exception e) {
    		System.err.print("Notifications retrieval failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Notifications retrieval failed.");
    	}
		
		
	}
    
    protected Response handleRegister(Request request){
    	System.out.println("Inside handle");
		User newUser = (User) request.getData("userObject");
		String uid = newUser.getId();
		
		String defaultPfpPath = "DefaultPfp/DefaultPfp.png";
		String userImgPath = "Users/" + uid + "/profile.png";
		
		// Save the user profile in the database
		try {
			DatabaseReference myRef = database.getReference().child("Users").child(uid);
	        myRef.setValueAsync(newUser).get();
	        
	        FirebaseStorageService storageService = FirebaseStorageService.getInstance();
	        Bucket bucket = storageService.getBucket();
	        
	        // Get default profile picture from storage
	        Blob defaultBlob = bucket.get(defaultPfpPath);
	        if (defaultBlob == null || !defaultBlob.exists()) {
	        	return new Response(false, "Couldn't fetch default profile picture.");
	        }
	        
	        byte[] defaultPfpBytes = defaultBlob.getContent();
	        
	        // Upload the image to the storage
	        bucket.create(userImgPath, defaultPfpBytes, "image/png");
	        
	        // Update image path in the database
	        myRef = database.getReference().child("Users").child(uid).child("/img");
	        myRef.setValueAsync(userImgPath).get();
	        
	        // Create a welcome notification for the user
	        NotificationService.getInstance().sendNotification(
	                uid,                                      // Target user (The newly registered user)
	                NotificationType.SIGNUP,                  // Notification type Enum
	                "2Bid",                                   // Sender ID
	                "2Bid",                                   // Sender Username
	                newUser.getImg(),                         // Path to the image
	                "Welcome to 2Bid! Start setting up your profile by exploring new biddings." 
	            );
            
	        return new Response(true, "Registration successfull.");
		} catch (Exception e) {
			System.err.print("Registration failed for UID: " + uid);
			e.printStackTrace();
			
			// Delete user Authentication data!
			if (uid != null) {
	            try {
	                FirebaseAuth.getInstance().deleteUser(uid);
	                System.err.println("Rollback successful: Deleted orphaned user " + uid);
	            } catch (FirebaseAuthException rollbackError) {
	                System.err.println("Rollback failed: Could not delete user " + uid);
	            }
	        }
			
			return new Response(false, "Uploading user data or media failed: " + e.getMessage());
		}
	}

    protected Response handleLogin(Request request) {
		try {
			String idToken = (String) request.getData("idToken");
			
			FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = token.getUid();
			
			System.out.println("Authenticated user: " + uid);
			return new Response(true, "Signin successfful");
			
		} catch (FirebaseAuthException e) {
			return new Response(false, "Invalid token");
		} catch (Exception e) {
    		System.err.print("Signin failed. ");
			e.printStackTrace();
			
			return new Response(false, "Signin failed: " + e.getMessage());
    	}
	}
    
    protected Response handleRemoveNotificationById(Request request) {
    	try {
    		String uid = (String) request.getData("uid");
    		String notificationId = (String) request.getData("notificationId");
    		
    		DatabaseReference myRef = database.getReference().child("Users").child(uid).child("notifications").child(notificationId);
    		myRef.setValueAsync(null).get();
    		
    		return new Response(true, "Notification removal succeeded.");
    	} catch (Exception e) {
    		System.err.print("Notification removal failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Notification removal failed.");
    	}
	}
    public Response handleGetUserPhone(Request request) {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final String[] phoneHolder = new String[1];
        String targetUid = (String) request.getData("targetUid");
        
        try {
            database.getReference().child("Users").child(targetUid).child("phoneNumber")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            phoneHolder[0] = snapshot.getValue(String.class);
                        }
                        latch.countDown();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        latch.countDown();
                    }
                });
                
            latch.await();
            
            if (phoneHolder[0] != null) {
                Response response = new Response(true, "Phone fetched successfully.");
                response.putData("phone", phoneHolder[0]);
                return response;
            } else {
                return new Response(false, "Phone number not found.");
            }
        } catch (Exception e) {
            return new Response(false, "Failed to fetch phone: " + e.getMessage());
        }
    }
}
