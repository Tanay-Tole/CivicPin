package com.spot.civicpin.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CivicLocationManager {

    public interface LocationCallback {
        void onSuccess(LocationResult locationResult);
        void onFailure(String errorMsg);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public CivicLocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallback callback) {
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processLocation(location, "GPS_DEVICE", callback);
                    } else {
                        // Fallback to last location
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                processLocation(lastLoc, "GPS_LAST_KNOWN", callback);
                            } else {
                                callback.onFailure("Location unavailable. Please select pin on map.");
                            }
                        }).addOnFailureListener(e -> callback.onFailure("Failed to obtain location: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Location request failed: " + e.getMessage()));
    }

    public void processLocation(Location location, String source, LocationCallback callback) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float accuracy = location.getAccuracy();

        new Thread(() -> {
            String addressStr = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", lat, lng);
            String localityStr = null;

            if (Geocoder.isPresent()) {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        if (addr.getMaxAddressLineIndex() >= 0) {
                            addressStr = addr.getAddressLine(0);
                        }
                        localityStr = BengaluruLocalityResolver.resolveLocality(addr);
                    }
                } catch (IOException e) {
                    // Geocoding failed, fallback to coordinates
                }
            }

            LocationResult result = new LocationResult(lat, lng, accuracy, addressStr, localityStr, source);
            callback.onSuccess(result);
        }).start();
    }
}
