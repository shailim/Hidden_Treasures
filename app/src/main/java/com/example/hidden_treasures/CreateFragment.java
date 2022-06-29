package com.example.hidden_treasures;

import static androidx.core.content.FileProvider.getUriForFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

public class CreateFragment extends Fragment {

    private static final String TAG = "CreateFragment";

    private File photoFile;
    private File videoFile;
    private EditText etTitle;
    private EditText etDescription;
    private Button btnSubmitMarker;
    private ImageView ivPreview;
    private VideoView vvPreview;
    private Button btnTakePicture;

    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private boolean locationPermissionGranted = false;
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        Log.i(TAG, "permission granted");
                        locationPermissionGranted = true;
                        getUserLocation();
                    } else {
                        Log.i(TAG, "permission denied");
                    }
                }
            });

    private ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result == true) {
                        Log.i(TAG, "took picture");
                        //decoding image
                        Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        Log.i(TAG, photoFile.getAbsolutePath());

                        // make image preview visible
                        ivPreview.setVisibility(View.VISIBLE);

                        //setting the image to the image preview in layout
                        ivPreview.setImageBitmap(takenImage);
                    } else {
                        Log.e(TAG, "picture wasn't taken");
                    }
                }
            });


    public CreateFragment() {
        // Required empty public constructor
    }

    public static CreateFragment newInstance() {
        CreateFragment fragment = new CreateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get location permission and user's current location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        getLocationPermission();

        // get references to all the views in layout
        ivPreview = view.findViewById(R.id.ivPreview);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        btnSubmitMarker = view.findViewById(R.id.btnSubmitMarker);
        btnTakePicture = view.findViewById(R.id.btnTakePicture);

        // set the onClick listeners for picture and video button
        setButtonListeners();
    }

    /* Checks if location permission is granted, requests permission if not */
    public void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getUserLocation();
        } else {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /* Finds the user's device location */
    @SuppressLint("MissingPermission")
    public void getUserLocation() {
        // first check if location permission is granted
        if (locationPermissionGranted) {
            Log.i(TAG, "getting device location");
            // get current location of device
            Task<Location> locationResult = fusedLocationProviderClient.getCurrentLocation(100, null); //param1: priority_high_accuracy
            // listen for when the task is completed
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        currentLocation = task.getResult();
                        Log.i(TAG, "found location");
                    }
                }
            });
        } else {
            Log.i(TAG, "permission is not granted to find location");
        }
    }


    /* Sets the onClickListeners for any buttons */
    private void setButtonListeners() {
        /* Take Picture */
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // getting a file reference
                photoFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");

                // wrapping File object into a content provider
                Uri fileProvider = getUriForFile(getContext(), getString(R.string.fileprovider_authority), photoFile);

                // launch intent to open camera
                cameraLauncher.launch(fileProvider);
            }
        });

        /* Submit Marker */
        btnSubmitMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // don't save marker is there's no title or picture/video
                if (etTitle.getText() == null || etTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
                } else if (photoFile == null && videoFile == null) {
                    Toast.makeText(getContext(), "A picture or video is required", Toast.LENGTH_SHORT).show();
                } else {
                    // get the values for marker
                    String title = etTitle.getText().toString();
                    String description = etDescription.getText().toString();
                    ParseFile file = new ParseFile(photoFile);
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

                    // create a new ParseMarker object
                    ParseMarker parseMarker = new ParseMarker(title, description, file, parseGeoPoint);

                    // call the async query to save marker
                    parseMarker.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.i(TAG, "marker created!");
                                Toast.makeText(getContext(), "marker created", Toast.LENGTH_SHORT).show();
                                //getActivity().getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, MapFragment.newInstance(title, description, currentLocation, file.getUrl())).commit();
                                MainActivity main = (MainActivity) getActivity();
                                main.switchTab(R.id.action_map, title, description, currentLocation, file.getUrl());
                            } else {
                                Log.e(TAG, "unable to save marker");
                                Log.i(TAG, e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

}