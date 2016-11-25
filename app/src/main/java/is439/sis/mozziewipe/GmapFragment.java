package is439.sis.mozziewipe;


import is439.sis.mozziewipe.support.CustomRequest;
import is439.sis.mozziewipe.support.VolleySingleton;
import com.android.volley.DefaultRetryPolicy;
import android.app.Fragment;
import android.content.Context;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.android.volley.Request;

import com.android.volley.Response;
import com.android.volley.VolleyError;

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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GmapFragment extends Fragment implements OnMapReadyCallback {

    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference biteRef;

    private GoogleMap mMap;
    // Declare a variable for the cluster manager.
    private ClusterManager<GeoItem> mClusterManager;
    private ClusterManager<GeoItem> sClusterManager;

    // UI Variable
    View view;
    Button searchBtn;
    EditText locationSearch;
    ProgressBar waitingSearch;

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

        locationSearch = (EditText) view.findViewById(R.id.editTextSearch);
        waitingSearch = (ProgressBar) view.findViewById(R.id.waitingSearch);
        searchBtn = (Button) view.findViewById(R.id.buttonSearch);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapSearch();
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
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));
        setUpCluster(currentLoc);
    }

    public void onMapSearch() {
        String location = locationSearch.getText().toString();
        // disable the text book, hide the search btn and show progress bar
        locationSearch.setEnabled(false);
        searchBtn.setVisibility(View.GONE);
        waitingSearch.setVisibility(View.VISIBLE);

        List<Address> addressList = null;

        if (!location.isEmpty()) {
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
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            // Invoke mosquito Activity API
            String url = getResources().getString(R.string.mosquito_activity_url);
            Map<String, Double> params = new HashMap<>();
            params.put("lat", address.getLatitude());
            params.put("long",  address.getLongitude());

            CustomRequest mosquitoActivityReq = new CustomRequest(Request.Method.POST, url, new JSONObject(params),
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            Log.i("mosquitoActivityReq API", "Response : " + response.toString());
                            setUpSearchCluster(response);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("mosquitoActivityReq API", "Error : " + error.toString());

                }
            });
            mosquitoActivityReq.setRetryPolicy(new DefaultRetryPolicy(50000,5,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleySingleton.getInstance(getActivity()).addToRequestQueue(mosquitoActivityReq);
        }
    }

    private void setUpCluster(LatLng currentLoc) {

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

    private void setUpSearchCluster(JSONArray clusterResponse) {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        sClusterManager = new ClusterManager<>(getActivity(), getMap());

        // Point the map's listeners at the listeners implemented by the cluster manager.
        getMap().setOnCameraIdleListener(sClusterManager);
        getMap().setOnMarkerClickListener(sClusterManager);

        try {
            // Add cluster items (markers) to the cluster manager.
            for(int i=0; i< clusterResponse.length(); i++) {
                double lat = clusterResponse.getJSONObject(i).optDouble("latitude");
                double lng = clusterResponse.getJSONObject(i).optDouble("longitude");
                GeoItem offsetItem = new GeoItem(lat, lng);
                sClusterManager.addItem(offsetItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            locationSearch.setEnabled(true);
            searchBtn.setVisibility(View.VISIBLE);
            waitingSearch.setVisibility(View.GONE);
        }

    }

    public GoogleMap getMap() {
        return mMap;
    }
}
