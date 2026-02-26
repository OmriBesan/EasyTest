package com.easydine.app.data.model;

import java.util.List;

public class Restaurant {

    // Keep fields private (clean)
    private String id;
    private String name;
    private String address;
    private String description;

    private double latitude;
    private double longitude;

    private float distanceToUser = 0.0f;

    // New fields
    private String placeId;
    private List<String> moods;

    public Restaurant() {}

    public Restaurant(String id, String name, String address, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.description = description;
    }

    // --- Getters / setters for strings ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setDescription(String description) { this.description = description; }

    // --- Lat/Lng ---
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // --- Distance ---
    public float getDistanceToUser() { return distanceToUser; }
    public void setDistanceToUser(float distanceToUser) { this.distanceToUser = distanceToUser; }

    // --- PlaceId ---
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    // --- Moods ---
    public List<String> getMoods() { return moods; }
    public void setMoods(List<String> moods) { this.moods = moods; }
}