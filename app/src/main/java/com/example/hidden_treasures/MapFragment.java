package com.example.hidden_treasures;

import android.Manifest;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
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
                        //repetitive, same three lines as in getLocationPermission() but I think I have to call these functions in the callback
                        // otherwise they could get called when the permission isn't granted yet
                        locationPermissionGranted = true;
                        updateLocationUI();
                        getDeviceLocation();
                    } else {
                        Log.i(TAG, "permission denied");
                    }
                }
            });

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    //will change later when I add params
    private String mParam1;
    private String mParam2;

    public MapFragment() {
        // Required empty public constructor
    }

    /*
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        //no parameters right now but can add later
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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
        this.map = googleMap;
        Log.i(TAG, "showing map");
        //request permission
        getLocationPermission(); // is there a way to do this synchronously instead of async
    }

    // checking if location permission is granted
    public void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            updateLocationUI();
            getDeviceLocation();
        } else {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    //adding this for now because it says I'm missing permission even though I added it and the permission request also works
    @SuppressLint("MissingPermission")
    // this function enables the location layer which shows the user's location on map
    public void updateLocationUI() {
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

    @SuppressLint("MissingPermission")
    public void getDeviceLocation() {
        if (locationPermissionGranted) {
            Log.i(TAG, "getting device location");
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            //using lastLocation() returns null the first time its called so use currentLocation()
            Task<Location> locationResult = fusedLocationProviderClient.getCurrentLocation(100, null); //param1: priority_high_accuracy
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            // move the camera to the current location
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), ZOOM_LEVEL));
                            Log.i(TAG, "showing current location");
                        } else {
                            Log.i(TAG, "lastLocation is null");
                        }
                    } else {
                        Log.i(TAG, "Current location is null");
                    }

                }
            });
        }
    }

}