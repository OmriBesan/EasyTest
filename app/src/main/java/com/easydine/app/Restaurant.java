package com.easydine.app;

public class Restaurant {
    public String id;
    public String name;
    public String address;
    public String description;

    public Restaurant() {}

    public Restaurant(String id, String name, String address, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.description = description;
    }
}
