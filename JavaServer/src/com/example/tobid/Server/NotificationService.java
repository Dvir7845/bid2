package com.example.tobid.Server;

import java.util.ArrayList;
import java.util.Calendar;

import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Singleton service class responsible for managing all notification-related operations,
 * including sending, fetching, and removing notifications.
 */
public class NotificationService {
    private static NotificationService instance;
    private final FirebaseDatabase database;

    private NotificationService() {
        this.database = FirebaseDatabase.getInstance();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Creates and sends a standard notification to a target user in Firebase.
     */
    public void sendNotification(String targetUid, NotificationType type, String senderId, 
                                 String senderUsername, String imgPath, String messageContent) {
        
        long timestamp = Calendar.getInstance().getTimeInMillis();
        String notificationId = senderId + "-" + timestamp;

        Notification notification = new Notification(
            type, notificationId, senderId, senderUsername, imgPath, messageContent
        );

        // Path: Users / {targetUid} / notifications / {notificationId}
        database.getReference()
                .child("Users")
                .child(targetUid)
                .child("notifications")
                .child(notificationId)
                .setValueAsync(notification);
        
        System.out.println("[NotificationService] Sent " + type + " to user: " + targetUid);
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
}