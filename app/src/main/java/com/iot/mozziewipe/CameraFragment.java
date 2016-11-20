package com.iot.mozziewipe;

import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iot.mozziewipe.support.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    static final int CAM_REQUEST = 1;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final double SCORE_THRESHOLD = 0.35;
    //Energy increase by
    final int ENERGY_INCREMENT = 4;
    final int POINTS_INCREMENT = 1;
    final int BONUS_POINTS = 3;
    Button captureBtn;
    // Firebase database instance variables
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    View view;
    private Uri fileUri;
    // Location
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;
    // UI Variable
    private Person person;
    private ImageView responseImage;
    private TextView displayText;
    private TextView captureText;
    private TextView scoreText;
    private ProgressBar waitingImage;

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

        // Load the initial UI
        responseImage = (ImageView) view.findViewById(R.id.swordImage);
        waitingImage = (ProgressBar) view.findViewById(R.id.waitingImage);
        displayText = (TextView) view.findViewById(R.id.displayText);
        captureText = (TextView) view.findViewById(R.id.captureText);
        scoreText = (TextView) view.findViewById(R.id.scoreText);

        responseImage.setImageResource(R.drawable.sword);
        displayText.setText(R.string.instruction_msg);
        captureText.setText(R.string.capture_pre);

        person = (Person) getArguments().getSerializable("person");

        captureBtn = (Button) view.findViewById(R.id.button_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scoreText.setText("");
                scoreText.setVisibility(View.GONE);
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = Uri.fromFile(getOutputMediaFile());
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(camera_intent, CAM_REQUEST);
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
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

                long timestamp = (System.currentTimeMillis() / 1000) * -1;
                String imageName = person.getPersonID() + timestamp + ".jpg";

                //Hide the image first and show loading bar
                loadingImage();

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
                DatabaseReference biteRef = database.getReference("bites/" + timestamp + "/" + person.getPersonID());
                Map mLocations = new HashMap();
                mLocations.put("image_Base64", base64Image);
                mLocations.put("image_name", imageName);
                mLocations.put("latitude", location.getLatitude());
                mLocations.put("longitude", location.getLongitude());
                biteRef.setValue(mLocations);

                //update user point and energy level
                DatabaseReference userRef = database.getReference("user/" + person.getPersonID());
                int nEnergy = person.getEnergy() + ENERGY_INCREMENT;
                int nPoint = person.getPoints() + POINTS_INCREMENT;
                if (nEnergy > 100) {
                    nEnergy = 100;
                    nPoint = nPoint + POINTS_INCREMENT;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("points", nPoint);
                updates.put("energy", nEnergy);
                userRef.updateChildren(updates);
                person.setPoints(nPoint);
                person.setEnergy(nEnergy);

                // upload to Firebase Storage
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://mozziewipe-6eaca.appspot.com").child("images/" + imageName);
                UploadTask uploadTask = storageRef.putFile(fileUri);

                // Manage upload to storage, what to do if failed / success
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        String imageName = taskSnapshot.getMetadata().getName();

                        // Invoke tensorflow RESTful API
                        String url = getResources().getString(R.string.image_score_url);

                        Map<String, String> params = new HashMap<>();
                        params.put("image_name", imageName);

                        JsonObjectRequest jsonRequest = new JsonObjectRequest(Method.POST, url, new JSONObject(params),
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            String scoreResponse = response.getString("score");
                                            double score = Double.valueOf(scoreResponse);
                                            Log.d("Call Image API", response.toString());
                                            scoreText.setText("Score : " + scoreResponse);
                                            if (score < SCORE_THRESHOLD) {
                                                nobiteImage();
                                            } else {
                                                successfulImage();
                                                // Add additional point for successful image, bonus.
                                                DatabaseReference userRef = database.getReference("user/" + person.getPersonID());
                                                int nPoint = person.getPoints() + BONUS_POINTS;
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("points", nPoint);
                                                userRef.updateChildren(updates);
                                                person.setPoints(nPoint);
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                //Current same as success
                                Log.i("Image Error.Response", error.toString());
                                successfulImage();
                            }
                        });
                        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(50000, 5,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        VolleySingleton.getInstance(getActivity()).addToRequestQueue(jsonRequest);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.i("firebase storage", "Failed to upload to firebase storage");
                        failedToUpload();
                    }
                });
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


    public void loadingImage() {
        waitingImage.setVisibility(View.VISIBLE);
        responseImage.setVisibility(View.GONE);
        captureBtn.setVisibility(View.GONE);
        captureText.setVisibility(View.GONE);

        displayText.setText(R.string.waiting_msg);
        captureText.setText(R.string.capture_post);
    }

    public void successfulImage() {
        waitingImage.setVisibility(View.GONE);
        responseImage.setVisibility(View.VISIBLE);
        captureBtn.setVisibility(View.VISIBLE);
        captureText.setVisibility(View.VISIBLE);
        scoreText.setVisibility(View.VISIBLE);

        responseImage.setImageResource(R.drawable.passed);
        displayText.setText(R.string.success_msg);
    }

    public void nobiteImage() {
        waitingImage.setVisibility(View.GONE);
        responseImage.setVisibility(View.VISIBLE);
        captureBtn.setVisibility(View.VISIBLE);
        captureText.setVisibility(View.VISIBLE);
        scoreText.setVisibility(View.VISIBLE);

        responseImage.setImageResource(R.drawable.nobite);
        displayText.setText(R.string.no_bite_msg);
    }

    public void failedToUpload() {
        waitingImage.setVisibility(View.GONE);
        responseImage.setVisibility(View.VISIBLE);
        captureBtn.setVisibility(View.VISIBLE);
        captureText.setVisibility(View.VISIBLE);

        responseImage.setImageResource(R.drawable.failed);
        displayText.setText(R.string.failed_msg);
    }

}
