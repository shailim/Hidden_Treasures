package com.example.hidden_treasures;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
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
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private List<ParseMarker> markers = new ArrayList<>();

    private GoogleMap map;

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.i(TAG, "showing map");
        this.map = googleMap;
        //setting initial position to show the whole world
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.52, 34.3), 0));
        //get markers from database and place on map
        getMarkers();
        // enables any markers on the map to be clickable
        enableMarkerClicks();
        // keep track of camera position on map
        trackCameraPosition();
    }

    /* retrieves markers from database, then calls a function to place markers on map */
    private void getMarkers() {
        ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
        markerQuery.addDescendingOrder("view_count");
        markerQuery.findInBackground(new FindCallback<ParseMarker>() {
            @Override
            public void done(List<ParseMarker> objects, ParseException e) {
                if (e == null) {
                    markers.addAll(objects);
                    Log.i(TAG, "got the markers");
                    // call function to place markers on map
                    placeMarkersOnMap();
                } else {
                    Log.i(TAG, "Couldn't get markers");
                }
            }
        });
    }

    /* Creates new markers and places them on the map */
    private void placeMarkersOnMap() {
        if (markers != null && markers.size() > 0) {
            for (ParseMarker marker : markers) {
                // get the marker values
                LatLng userLocation = new LatLng(marker.getLocation().getLatitude(), marker.getLocation().getLongitude());
                String title = marker.getTitle();
                String description = marker.getDescription();
                ParseFile media = marker.getMedia();
                //create a new marker object with the values
                Marker createdMarker = map.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title(title));
                // set the tag as the image url
                createdMarker.setTag(media.getUrl());
                createdMarker.setSnippet(description);
            }
            Log.i(TAG, "placed markers on map");
        }
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


    /* Adds a listener to track camera movement on map */
    public void trackCameraPosition() {
        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
            }
        });
    }

}