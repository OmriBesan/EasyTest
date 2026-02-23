package com.easydine.app.data.model;

public class Restaurant {
    public String id;
    public String name;
    public String address;
    public String description;
    private double latitude;
    private double longitude;
    private float distanceToUser = 0.0f ;

    public Restaurant() {}

    public Restaurant(String id, String name, String address, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.description = description;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public float getDistanceToUser() { return distanceToUser; }
    public void setDistanceToUser(float distance) { this.distanceToUser = distance; }

    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
