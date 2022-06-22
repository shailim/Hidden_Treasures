package com.example.hidden_treasures;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private GoogleMap map;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastKnownLocation;
    private static final float ZOOM_LEVEL = 15;

    private boolean locationPermissionGranted = false;
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        Log.i(TAG, "permission granted");
                        locationPermissionGranted = true;
                        enableLocationLayer();
                        findAndSetCurrentLocation();
                    } else {
                        Log.i(TAG, "permission denied");
                    }
                }
            });

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
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
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // to get the device's location
        // constructing the client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        // statically adding the map fragment, a child fragment of this class
        SupportMapFragment childMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map);

        // getting map
        if (childMapFragment != null) {
            childMapFragment.getMapAsync(this);
        }

        // return the layout view
        return view;
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.i(TAG, "showing map");
        this.map = googleMap;
        //setting initial position to last known location
        if (lastKnownLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), ZOOM_LEVEL));
        }
        //request permission to get location
        getLocationPermission();
        // enables any markers on the map to be clickable
        enableMarkerClicks();
    }

    /* Checks if location permission is granted, requests permission if not */
    public void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            enableLocationLayer();
            findAndSetCurrentLocation();
        } else {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    /* Enables the location layer on the map, needed to show user's location */
    @SuppressLint("MissingPermission")
    public void enableLocationLayer() {
        if (locationPermissionGranted) {
            //enabling the location layer
            map.setMyLocationEnabled(true);
            //when the button is enabled, the camera position moves to the user's location and centers it on the map
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            map.setMyLocationEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            lastKnownLocation = null;
            //request for permission
            getLocationPermission();
        }
    }

    /* Finds the user's device location and sets the map to that location */
    @SuppressLint("MissingPermission")
    public void findAndSetCurrentLocation() {
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
                        lastKnownLocation = task.getResult();
                        Log.i(TAG, "found location");
                        // place a marker at user's location
                        placeMarker(lastKnownLocation);
                        // moves the map's camera position to the user's location
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), ZOOM_LEVEL));
                    }
                }
            });
        } else {
            Log.i(TAG, "permission is not granted to find location");
        }
    }

    /* Places a marker at the user's current location
     * Takes in a location value */
    public void placeMarker(Location location) {
        // get the location coordinates and create a LatLng object
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        // add the new marker at the location
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Current Location"));
    }


    /* Adds a click listener on markers
     * Will update functionality of click later  */
    public void enableMarkerClicks() {
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                // shows a toast with the marker title
                Toast.makeText(getContext(), marker.getTitle(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}