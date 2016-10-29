package com.iot.mozziewipe;


import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;


public class GmapFragment extends Fragment implements OnMapReadyCallback {

    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference biteRef;

    private GoogleMap mMap;
    // Declare a variable for the cluster manager.
    private ClusterManager<GeoItem> mClusterManager;

    public GmapFragment() {
        // Required empty public constructor
    }

    public static GmapFragment newInstance(double latitude, double longitude) {
        GmapFragment fragment = new GmapFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble("latitude", latitude);
        bundle.putDouble("longitude", longitude);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gmap, container, false);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        MapFragment mapFragment = (MapFragment)getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng currentLoc = new LatLng(getArguments().getDouble("latitude"), getArguments().getDouble("longitude"));
        mMap.addMarker(new MarkerOptions()
                .position(currentLoc)
                .title("Your Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));
        setUpClusterer(currentLoc);
    }

    private void setUpClusterer(LatLng currentLoc) {

        // Position the map. Zoom level
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 15));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<GeoItem>(getContext(), getMap());

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        getMap().setOnCameraIdleListener(mClusterManager);
        getMap().setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    //Need to change to retrieve from firebase
    private void addItems() {

        long currentTS = (System.currentTimeMillis()/1000);
        final long threeHrAgo = (currentTS - (3 * 3600)) * -1;

        biteRef = database.getReference("bites");
        biteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot filterSnapshot: dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    long ts = Long.parseLong(filterSnapshot.getKey());
                    if(ts <= threeHrAgo) {
                        for(DataSnapshot postSnapshot : filterSnapshot.getChildren()) {
                            double lat = (double) postSnapshot.child("latitude").getValue();
                            double lng = (double) postSnapshot.child("longitude").getValue();
                            GeoItem offsetItem = new GeoItem(lat, lng);
                            mClusterManager.addItem(offsetItem);
                        }
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public GoogleMap getMap() {
        return mMap;
    }
}
