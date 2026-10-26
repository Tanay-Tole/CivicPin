package com.spot.civicpin.location;

public class LocationResult {
    public double latitude;
    public double longitude;
    public float accuracy;
    public long timestamp;
    public String address;
    public String locality;
    public String source; // "GPS_DEVICE", "PHOTO_EXIF", "USER_MAP_PIN"

    public LocationResult(double latitude, double longitude, float accuracy, String address, String locality, String source) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = System.currentTimeMillis();
        this.address = address;
        this.locality = locality;
        this.source = source;
    }
}
