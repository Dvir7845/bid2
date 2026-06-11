package com.example.tobid.DataModels;

import java.io.Serializable;

/**
 * Represents a user in the application.
 * This class contains the user's ID, email, phone number, username, and the path to their profile picture in Firebase Storage.
 */
public class User implements Serializable {
    private final String id; // The unique identifier for the user.
    private final String email;
    private String username;
    private String phoneNumber;

    /**
     * The path to the user's profile picture stored in Firebase Storage.
     */
    private String img;

    /**
     * Empty constructor required for using the adapter and other internal purposes.
     * Initializes the `id` and `email` fields to empty strings.
     */
    public User() {
        this.id = "";
        this.email = "";
    }

    /**
     * Constructor for creating a user with all required fields.
     *
     * @param id       The unique identifier of the user.
     * @param email    The email address of the user.
     * @param username The username chosen by the user.
     * @param phoneNumber the phone number chosen by the user
     * @param img      The string path to the user's profile picture in Firebase Storage.
     */
    public User(String id, String email, String phoneNumber, String username, String img) {
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.img = img;
    }

    public String getId() {
        return id;
    }
    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the path to the user's profile picture stored in Firebase Storage.
     *
     * @return The string representing the image path in Firebase Storage.
     */
    public String getImg() {
        return img;
    }

    /**
     * Sets the path to the user's profile picture stored in Firebase Storage.
     *
     * @param img The string representing the new image path in Firebase Storage.
     */
    public void setImg(String img) {
        this.img = img;
    }
}
