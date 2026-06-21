package com.example.tobid.Server;

import java.util.Calendar;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.google.firebase.database.FirebaseDatabase;

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
}