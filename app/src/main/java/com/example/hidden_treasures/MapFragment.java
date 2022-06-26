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
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private List<ParseMarker> markers = new ArrayList<>();

    private GoogleMap map;
    private SearchView searchLocation;

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

        // enable autocomplete search for locations
        setupAutocompleteSearch();

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
                geocoder.getFromLocationName(place.getName(), 1, new Geocoder.GeocodeListener() {

                    @Override
                    public void onGeocode(@NonNull List<Address> list) {
                        // if there was a place returned for the search
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
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
                                    }
                                });
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

}