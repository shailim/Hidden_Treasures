package com.example.hidden_treasures.createMarker;

import static androidx.core.content.FileProvider.getUriForFile;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.hidden_treasures.MainActivity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.roger.catloadinglibrary.CatLoadingView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.sql.Date;
import java.util.UUID;

public class CreateFragment extends Fragment {

    private static final String TAG = "CreateFragment";

    private MarkerViewModel markerViewModel;

    public static String BITMAP_IMAGE = "takenImage";
    public static String PHOTO_FILE = "photoFile";

    private CatLoadingView mCatProgressView;

    /* View related variables */
    private File photoFile;
    private Bitmap takenImage;
    private EditText etTitle;
    private ImageButton backToCamera;
    private ImageButton btnSubmitMarker;

    /* Location related variables */
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


    public CreateFragment() {
        // Required empty public constructor
    }

    /* A bitmap image and photo file will be passed as arguments from camera fragment */
    public static CreateFragment newInstance(Bitmap takenImage, File photoFile) {
        CreateFragment fragment = new CreateFragment();
        Bundle args = new Bundle();
        args.putParcelable(BITMAP_IMAGE, takenImage);
        args.putSerializable(PHOTO_FILE, photoFile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        markerViewModel = new ViewModelProvider(this).get(MarkerViewModel.class);
        if (getArguments() != null) {
            photoFile = (File) getArguments().getSerializable(PHOTO_FILE);
            takenImage = getArguments().getParcelable(BITMAP_IMAGE);
        }
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

        // initialize progress bar
        mCatProgressView = new CatLoadingView();

        // hide nav bar in this create fragment
        getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);

        // get location permission and user's current location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        getLocationPermission();

        // get references to all the views in layout
        ImageView ivPreview = view.findViewById(R.id.ivPreview);
        etTitle = view.findViewById(R.id.etTitle);
        backToCamera = view.findViewById(R.id.backToCamera);
        btnSubmitMarker = view.findViewById(R.id.btnSubmitMarker);

        // show the image in the image preview
        ivPreview.setImageBitmap(takenImage);

        // set the onClick listeners for button views
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
            int priority_high_accuracy = 100;
            Task<Location> locationResult = fusedLocationProviderClient.getCurrentLocation(priority_high_accuracy, null);
            // listen for when the task is completed
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // save user's location
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

        /* button to return to camera fragment */
        backToCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "clicked on back to camera");
                // show the navbar again
                getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

                // Go back to camera fragment which is the previous fragment
                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.beginTransaction().remove(CreateFragment.this);
                    fm.popBackStackImmediate();
                } else {
                    Log.i(TAG, "no fragment to go back to");
                }
            }
        });

        /* submit button to create markers */
        btnSubmitMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressBar();

                // don't save marker is there's no title or picture
                if (etTitle.getText() == null || etTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
                } else if (photoFile == null) {
                    Toast.makeText(getContext(), "A picture is required", Toast.LENGTH_SHORT).show();
                } else {
                    // get the values for marker
                    String title = etTitle.getText().toString();
                    ParseFile file = new ParseFile(photoFile);
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

                    saveMarkerToParse(title, file, parseGeoPoint);

                    // adding the new marker to the local database
                    long millis = System.currentTimeMillis();
                    MarkerEntity newMarker = new MarkerEntity(UUID.randomUUID().toString(),
                            millis, title, currentLocation.getLatitude(),
                            currentLocation.getLongitude(), "https://picsum.photos/200/300", ParseUser.getCurrentUser().toString(), 0, 0);
                    markerViewModel.insertMarker(newMarker);
                }
            }
        });
    }

    /* Uploads markers to database in Parse */
    private void saveMarkerToParse(String title, ParseFile file, ParseGeoPoint parseGeoPoint) {
        // create a new ParseMarker object
        ParseMarker parseMarker = new ParseMarker(title, file, parseGeoPoint);

        // call the async query to save marker
        parseMarker.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.i(TAG, "marker created!");

                    // remove this fragment
                    FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                    ft.remove(CreateFragment.this);
                    ft.commit();

                    // remove progress bar
                    mCatProgressView.dismiss();

                    // posting new marker event
                    EventBus.getDefault().post(new NewMarkerEvent(parseMarker));

                } else {
                    Log.e(TAG, "unable to save marker");
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }

    /* Displays progress bar */
    private void showProgressBar() {
        mCatProgressView.show(getActivity().getSupportFragmentManager(), "");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}