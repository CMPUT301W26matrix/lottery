package com.example.lottery.model;

/**
 * User model
 * Represents an entrant/user in the system.
 * This class stores basic personal information.
 */
public class User {

    private String userId;
    private String name;
    private String email;
    private String phoneNumber;

    // Empty constructor required for Firestore
    public User() {
    }

    public User(String name, String email, String phoneNumber) {
        this(null, name, email, phoneNumber);
    }

    public User(String userId, String name, String email, String phoneNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
