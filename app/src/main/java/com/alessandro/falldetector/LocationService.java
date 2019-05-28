package com.alessandro.falldetector;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;


public class LocationService extends IntentService implements ConnectionCallbacks, OnConnectionFailedListener {

    public static final String LOCATION_FOUND = "LOCATION_FOUND";
    public static final String LOCATION_NOT_FOUND = "LOCATION_NOT_FOUND";

    public static final String CURRENT_LOCATION = "com.alessandro.falldetector.CURRENT_LOCATION";

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;


    public LocationService() {
        super(LocationService.class.getName());
    }

    public LocationService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (checkPlayServices()) {
            buildGoogleApiClient();
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }else{
            Intent i = new Intent(LOCATION_NOT_FOUND);
            LocationService.this.sendBroadcast(i);
        }
    }


    private void returnLocation() {

        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mCurrentLocation != null) {
            Intent i = new Intent(LOCATION_FOUND);
            i.putExtra(CURRENT_LOCATION, mCurrentLocation);
            LocationService.this.sendBroadcast(i);
        } else {
            Intent i = new Intent(LOCATION_NOT_FOUND);
            LocationService.this.sendBroadcast(i);
        }
    }
    /**
     * Creating google api client instance
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG);
            } else {
                Toast.makeText(getApplicationContext(),"This device is not supported.", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    //Google CallBack Method
    @Override
    public void onConnected(Bundle bundle) {
        returnLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        Intent i = new Intent(LOCATION_NOT_FOUND);
        LocationService.this.sendBroadcast(i);
    }
}

