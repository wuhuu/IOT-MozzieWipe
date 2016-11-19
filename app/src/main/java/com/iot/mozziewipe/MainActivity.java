package com.iot.mozziewipe;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final int ENERGY_DEDUCTION = 2;

    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    // Location
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private Person person;
    private BottomBar bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        Intent intent = getIntent();
        person = (Person)intent.getSerializableExtra("person");

        bottomBar = BottomBar.attach(this, savedInstanceState);

        bottomBar.setDefaultTabPosition(1);
        bottomBar.setItems(R.menu.menu);

        bottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                FragmentManager fm = getFragmentManager();
                if (menuItemId == R.id.menu_camera) {
                    // The user selected the "camera" tab.
                    System.out.println("camera");
                    CameraFragment fragment = CameraFragment.newInstance(person);
                    fm.beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                } else if (menuItemId == R.id.menu_map){
                    System.out.println("Map");
                    int nEnergy = person.getEnergy() - ENERGY_DEDUCTION;
                    if(nEnergy > 0) {
                        //update the energy
                        person.setEnergy(nEnergy);
                        DatabaseReference userRef = database.getReference("user/" + person.getPersonID());
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("energy", nEnergy);
                        userRef.updateChildren(updates);

                        GmapFragment fragment = GmapFragment.newInstance(location.getLatitude(), location.getLongitude());
                        fm.beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                    } else {
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout),"Sorry, you don't have enough energy.",
                                Snackbar.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    System.out.println("Profile");
                    ProfileFragment fragment = ProfileFragment.newInstance(person);
                    fm.beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {
                if (menuItemId == R.id.menu_map){
                    int nEnergy = person.getEnergy() - ENERGY_DEDUCTION;
                    if(nEnergy < 0) {
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout),"Sorry, you don't have enough energy.",
                                Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("MainActivity", "Location services connected.");
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            System.out.println(String.valueOf(location.getLatitude()));
            System.out.println(String.valueOf(location.getLongitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("MainActivity", "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getParent(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("MainActivity", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (LocationListener) this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
    }
}
