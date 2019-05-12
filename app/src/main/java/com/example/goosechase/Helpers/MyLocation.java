package com.example.goosechase.Helpers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.goosechase.DelegateInterface.LocationDelegate;
import com.example.goosechase.DelegateInterface.PermissionDelegate;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

public class MyLocation {
    public static final String TAG = MyLocation.class.getSimpleName();

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static float DEFAULT_ZOOM;
    private boolean mLocationsPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Activity activity;
    private double lat = -1;
    private double lng = -1;
    private static MyLocation instance = new MyLocation();

    private MyLocation() {}

    public static MyLocation getInstance() {
        return instance;
    }
    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }

    public void getLocationPermission(PermissionDelegate delegate) {
        String[] permission = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(activity, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationsPermissionsGranted = true;
                delegate.taskCompletionResult(mLocationsPermissionsGranted);
            } else {
                ActivityCompat.requestPermissions(activity, permission, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(activity, permission, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void getDeviceLocation(final LocationDelegate delegate) {

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.activity);
        try {
            if (mLocationsPermissionsGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        if (currentLocation == null) {
                            Log.e(TAG, "Location not found.");
                            return;
                        }
                        //TODO ADD BACK IN
                        Double myLat = currentLocation.getLatitude();
                        Double myLng = currentLocation.getLongitude();
                        lat = myLat;
                        lng = myLng;
                        delegate.taskCompletionResult(myLat, myLng);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException");
        }
    }
}