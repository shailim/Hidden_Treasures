package com.example.hidden_treasures;

import static com.parse.Parse.getApplicationContext;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.dynamic.SupportFragmentWrapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private static final String PREV_LATITUDE = "prevLatitude";
    private static final String PREV_LONGITUDE = "prevLogitude";
    private static final String ZOOM_LEVEL = "prevZoomLevel";

    private double prevLatitude;
    private double prevLongitude;
    private float zoomLevel;

    private String createdMarkerTitle;
    private String createdMarkerDescription;
    private Location createdMarkerLocation;
    private String createdMarkerMediaUrl;

    private List<ParseMarker> markers = new ArrayList<>();

    private GoogleMap map;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    public static MapFragment newInstance(String title, String description, Location location, String mediaUrl) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ParseMarker.TITLE, title);
        args.putString(ParseMarker.DESCRIPTION, description);
        args.putParcelable(ParseMarker.LOCATION, location);
        args.putString(ParseMarker.MEDIA, mediaUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            createdMarkerTitle = getArguments().getString(ParseMarker.TITLE);
            createdMarkerDescription = getArguments().getString(ParseMarker.DESCRIPTION);
            createdMarkerLocation = getArguments().getParcelable(ParseMarker.LOCATION);
            createdMarkerMediaUrl = getArguments().getString(ParseMarker.MEDIA);
        }
        if (savedInstanceState != null) {
            prevLatitude = savedInstanceState.getDouble(PREV_LATITUDE);
            prevLongitude = savedInstanceState.getDouble(PREV_LONGITUDE);
            zoomLevel = savedInstanceState.getFloat(ZOOM_LEVEL);
        } else {
            prevLatitude = 37.0902;
            prevLongitude = -95.7129;
            zoomLevel = 5;
        }
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

        // enable autocomplete search for locations
        setupAutocompleteSearch();

        // return the layout view
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "on save instance being called");
        outState.putDouble(PREV_LATITUDE, map.getCameraPosition().target.latitude);
        outState.putDouble(PREV_LONGITUDE, map.getCameraPosition().target.longitude);
        outState.putFloat(ZOOM_LEVEL, map.getCameraPosition().zoom);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.map_style_json)));
        Log.i(TAG, "showing map");
//        if (createdMarkerTitle != null) {
//            LatLng userLocation = new LatLng(createdMarkerLocation.getLatitude(), createdMarkerLocation.getLongitude());
//            Marker createdMarker = map.addMarker(new MarkerOptions()
//                    .position(userLocation)
//                    .title(createdMarkerTitle));
//            // set the tag as the image url
//            createdMarker.setTag(createdMarkerMediaUrl);
//            createdMarker.setSnippet(createdMarkerDescription);
//            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));
//        } else {
            // initial position should show where the user was previously looking
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(prevLatitude, prevLongitude), zoomLevel));
       // }
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

    public void addCreatedMarker(String title, String description, Location location, String imageUrl) {
        if (title != null) {
            //LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            LatLng userLocation = new LatLng(48.8566, 2.3522);
            Marker createdMarker = map.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title(title));
            // set the tag as the image url
            createdMarker.setTag(imageUrl);
            createdMarker.setSnippet(description);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));
        }
    }

    /* Adds a click listener on markers
     * On marker click, a marker detail view opens up  */
    public void enableMarkerClicks() {
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                // to set marker detail as a child fragment
                FragmentManager childFragMan = getChildFragmentManager();
                FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                // create a new marker detail fragment instance and pass in image url, place name, description
                MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance((String) marker.getTag(), marker.getTitle(), marker.getSnippet());
                // add the child fragment to current map fragment
                childFragTrans.add(R.id.mapFragmentLayout, markerDetailFrag);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
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

    /* Uses Google's Places API to display place name for autocomplete searching */
    public void setupAutocompleteSearch() {
        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));
        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(getContext());

        // Initialize the AutocompleteSupportFragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // create a Geocoder object to get coordinates of place that user searched for
                Geocoder geocoder = new Geocoder(getContext(), new Locale("en"));
                try {
                    List<Address> list = geocoder.getFromLocationName(place.getName(), 1);
                    if (!list.isEmpty()) {
                            // get the coordinates of the location
                            Double latitude = list.get(0).getLatitude();
                            Double longitude = list.get(0).getLongitude();

                            // move map camera position to the location
                            // right now, it's on a different thread so have to do it on the UI thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 10));
                                    }
                                });
                            }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

}