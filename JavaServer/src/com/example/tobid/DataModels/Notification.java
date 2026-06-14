package com.example.tobid.DataModels;

import java.io.Serializable;

/**
 * Represents a notification for a user within the application.
 * A notification includes the sender's details (id, username, profile picture)
 * and the message content associated with the notification.
 */
public class Notification implements Serializable{
    private NotificationType notificationType;
    // Unique identifier for the notification (senderID + creationTime)
    private String id;
    private String senderId;
    private String senderUsername;

    // A string path to the sender's profile picture in Firebase Storage
    private String senderImg;

    // The notification message (the content of the notification)
    private String message;

    /**
     * Empty constructor for Notification. This is used when creating instances of
     * Notification through an adapter or when deserializing data.
     */
    public Notification() {}

    /**
     * Constructor for a Notification object.
     *
     * @param id the unique notification ID (constructed from senderId + creation time in millis)
     * @param senderId the user ID of the sender
     * @param senderUsername the username of the sender
     * @param senderImg the string path to the sender's profile image in FirebaseStorage
     * @param message the message content of the notification
     */
    public Notification(NotificationType notificationType, String id, String senderId,
                        String senderUsername, String senderImg, String message) {
        this.notificationType = notificationType;
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderImg = senderImg;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderImg() {
        return senderImg;
    }

    public void setSenderImg(String senderImg) {
        this.senderImg = senderImg;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
