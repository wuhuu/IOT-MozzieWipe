package com.iot.mozziewipe;


import android.app.Fragment;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.List;


public class GmapFragment extends Fragment implements OnMapReadyCallback {

    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference biteRef;

    private GoogleMap mMap;
    // Declare a variable for the cluster manager.
    private ClusterManager<GeoItem> mClusterManager;

    // UI Variable
    View view;
    Button searchBtn;
    EditText locationSearch;

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
        view =  inflater.inflate(R.layout.fragment_gmap, container, false);

        searchBtn = (Button) view.findViewById(R.id.buttonSearch);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapSearch(view);
                // hide the keyboard
                try {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // Add a marker in Sydney and move the camera
        LatLng currentLoc = new LatLng(getArguments().getDouble("latitude"), getArguments().getDouble("longitude"));
        mMap.addMarker(new MarkerOptions()
                .position(currentLoc)
                .title(getResources().getString(R.string.currentLoc))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));
        setUpClusterer(currentLoc);
    }

    public void onMapSearch(View view) {
        EditText locationSearch = (EditText) view.findViewById(R.id.editTextSearch);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(getActivity());
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.searchLoc))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .draggable(true));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void setUpClusterer(LatLng currentLoc) {

        // Position the map. Zoom level
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 15));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<>(getActivity(), getMap());

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        getMap().setOnCameraIdleListener(mClusterManager);
        getMap().setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems(mClusterManager);
    }

    //Need to change to retrieve from firebase
    private void addItems(final ClusterManager mClusterManager) {

        final long currentTS = (System.currentTimeMillis() / 1000);
        final long threeHrAgo = (currentTS - (3 * 3600)) * -1;

        biteRef = database.getReference("bites");
        biteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot filterSnapshot : dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    long ts = Long.parseLong(filterSnapshot.getKey());
                    if (ts <= threeHrAgo) {
                        for (DataSnapshot postSnapshot : filterSnapshot.getChildren()) {
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
