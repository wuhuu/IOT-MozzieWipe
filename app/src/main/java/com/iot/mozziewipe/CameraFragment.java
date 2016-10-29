package com.iot.mozziewipe;


import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    //Energy increase by
    final int ENERGY_INCREMENT = 10;
    final int POINTS_INCREMENT = 1;

    static final int CAM_REQUEST = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    Button captureBtn;

    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private Uri fileUri;

    // Location
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;

    private Person person;

    View view;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance(Person person) {
        CameraFragment fragment = new CameraFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("person", person);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_camera, container, false);

        person = (Person) getArguments().getSerializable("person");

        captureBtn = (Button) view.findViewById(R.id.button_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = Uri.fromFile(getOutputMediaFile());
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(camera_intent, CAM_REQUEST);
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds

        return view;
    }

    private File getOutputMediaFile() {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MozzieWipeApp");

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File image_file = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return image_file;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAM_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                // successfully captured the image
                // display it in image view
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8; // shrink it down otherwise we will use stupid amounts of memory
                Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] bytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

                //upload to Firebase Database
                long timestamp = (System.currentTimeMillis()/1000) * -1;
                DatabaseReference biteRef = database.getReference("bites/" + timestamp + "/" + person.getPersonID());
                Map mLocations = new HashMap();
                mLocations.put("image_Base64", base64Image);
                mLocations.put("image_name", fileUri.getLastPathSegment());
                mLocations.put("latitude", location.getLatitude());
                mLocations.put("longitude", location.getLongitude());
                biteRef.setValue(mLocations);

                //update user point and energy level
                DatabaseReference userRef = database.getReference("user/" + person.getPersonID());
                int nEnergy = person.getEnergy() + ENERGY_INCREMENT;
                int nPoint = person.getPoints() + POINTS_INCREMENT;
                Map<String, Object> updates = new HashMap<>();
                updates.put("points", nPoint);
                updates.put("energy", nEnergy);
                userRef.updateChildren(updates);
                person.setPoints(nPoint);
                person.setEnergy(nEnergy);

                // upload to Firebase Storage
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://mozziewipe-6eaca.appspot.com");
                StorageReference storeageRef = storageRef.child("images/"+fileUri.getLastPathSegment());
                storeageRef.putFile(fileUri);
                TextView displayText = (TextView) view.findViewById(R.id.displayText);
                displayText.setText("Thank you for the submission");
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("CameraFragment", "Location services connected.");
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
        Log.i("CameraFragment", "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("CameraFragment", "Location services connection failed with code " + connectionResult.getErrorCode());
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
