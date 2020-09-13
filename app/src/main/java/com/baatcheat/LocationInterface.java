package com.baatcheat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

class LocationInterface {

    private final String TAG="LOCATIONINTERFACE";

    public static double lastUpdatedLongitude=0.0;
    public static double lastUpdatedLatitude=0.0;
    void getLocationData(Context context, final MyCallBack myCallBack){
        //request location first
        //https://stackoverflow.com/a/50448772/7406257
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
            }
        };
        LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(mLocationRequest, mLocationCallback, null);

        //update the last known location and send to database
        if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "GPS probably off");
            Toast.makeText(context,"I need GPS Permission to search people nearby!",Toast.LENGTH_LONG).show();
        }
        else {
            Log.d(TAG,"We are getting last location");
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if(location!=null){
                                double currentLatitude = location.getLatitude();
                                double currentLongitude = location.getLongitude();

                                lastUpdatedLatitude=currentLatitude;
                                lastUpdatedLongitude=currentLongitude;
                                Log.d(TAG,"LAT:"+currentLatitude+"\tLONG:"+currentLongitude);

                                double[] locationStored={currentLatitude,currentLongitude};
                                myCallBack.onLocationCallback(locationStored);
                            }
                            else {
                                Log.d(TAG,"OUR LOCATION IS NULL!!!");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, String.valueOf(e));
                        }
                    });



        }

    }


}
