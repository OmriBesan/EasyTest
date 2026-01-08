package com.easydine.app.data.model; // <--- Make sure this matches your folder!

import com.google.firebase.Timestamp;

public class Booking {
    private String bookingId;
    private String userId;
    private String restaurantId;
    private String date;
    private String time;
    private int partySize;
    private Timestamp createdAt;

    public Booking() {} // Empty constructor for Firestore

    public Booking(String bookingId, String userId, String restaurantId, String date, String time, int partySize) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.date = date;
        this.time = time;
        this.partySize = partySize;
        this.createdAt = Timestamp.now();
    }

    public String getBookingId() { return bookingId; }
    public String getUserId() { return userId; }
    public String getRestaurantId() { return restaurantId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public int getPartySize() { return partySize; }
    public Timestamp getCreatedAt() { return createdAt; }

    // --- THIS IS THE METHOD YOU WERE MISSING ---
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
}