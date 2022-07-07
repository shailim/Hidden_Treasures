package com.example.hidden_treasures.map;

import static com.parse.Parse.getApplicationContext;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private List<String> markerIDs = new ArrayList<>();
    private ClusterManager<MarkerItem> clusterManager;
    private GoogleMap map;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
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
        Log.i(TAG, "on save instance being called");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        // set map style
        map.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.map_style_json)));
        Log.i(TAG, "showing map");
        // initialize the cluster manager
        clusterManager = new ClusterManager<MarkerItem>(getContext(), map);
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
        clusterManager.setAnimation(false);
        // set initial position of map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.4530, -122.1817), 10));

        /* To get initial markers */
        ParseGeoPoint southwestBound = new ParseGeoPoint(37.4530 - 5, -122.1817 - 5);
        ParseGeoPoint northeastBound = new ParseGeoPoint(37.4530 + 5, -122.1817 + 5);
        //get markers from database and place on map
        getMarkers(50, southwestBound, northeastBound);

        // enables any markers on the map to be clickable
        enableMarkerClicks();
        // listen for whenever camera is idle on map
        watchCameraIdle();
    }

    /* retrieves markers from database, then calls a function to place markers on map */
    private void getMarkers(int numMarkersToGet, ParseGeoPoint southwestBound, ParseGeoPoint northeastBound) {
        ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
        markerQuery.setLimit(numMarkersToGet);
        // restricting markers to a rectangular bounding box
        markerQuery.whereWithinGeoBox("location", southwestBound, northeastBound);
        // not getting repeated markers
        markerQuery.whereNotContainedIn("objectId", markerIDs);
        markerQuery.findInBackground(new FindCallback<ParseMarker>() {
            @Override
            public void done(List<ParseMarker> objects, ParseException e) {
                if (e == null) {
                    for (ParseMarker marker : objects) {
                        markerIDs.add(marker.getObjectId());
                    }
                    Log.i(TAG, String.valueOf(markerIDs.size()));
                    Log.i(TAG, "got the markers");
                    // call function to place markers on map
                    placeMarkersOnMap(objects);
                } else {
                    Log.i(TAG, "Couldn't get markers");
                }
            }
        });
    }

    /* Creates new markers and places them on the map */
    private void placeMarkersOnMap(List<ParseMarker> markers) {
        if (markers != null && markers.size() > 0) {
            // id is used to get random images
            int id = 1;
            for (ParseMarker marker : markers) {
                // get the marker values
                LatLng userLocation = new LatLng(marker.getLocation().getLatitude(), marker.getLocation().getLongitude());
                String title = marker.getTitle();
                ParseFile media = marker.getMedia();

                // TODO: replace random images with the real media url
                // generate random images for markers for now for the test data
                String url = "https://picsum.photos/id/" + id + "/200/300";

                // create a new marker item and add it to the cluster manager
                MarkerItem newMarker = new MarkerItem(userLocation.latitude, userLocation.longitude, title, url);
                clusterManager.addItem(newMarker);
                id++;
            }
            clusterManager.cluster();
            Log.i(TAG, "placed markers on map");
        }
    }

    /* adds an individual newly created marker to the cluster manager */
    public void addCreatedMarker(String title, Location location, String imageUrl) {
        MarkerItem newMarker = new MarkerItem(location.getLatitude(), location.getLongitude(), title, imageUrl);
        clusterManager.addItem(newMarker);
        Log.i(TAG, "moving camera to new marker");
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.8566, 2.3522), 13));
    }

    /* Adds a click listener on markers
     * On marker click, a marker detail view opens up  */
    public void enableMarkerClicks() {
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MarkerItem>() {
            @Override
            public boolean onClusterItemClick(MarkerItem item) {
                // to set marker detail as a child fragment
                FragmentManager childFragMan = getChildFragmentManager();
                FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                // create a new marker detail fragment instance and pass in image url, title
                MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance((String) item.getTag(), item.getTitle());
                // add the child fragment to current map fragment
                childFragTrans.add(R.id.mapFragmentLayout, markerDetailFrag);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
                return false;
            }
        });
    }


    /* Adds a listener to track when camera is idle on map */
    public void watchCameraIdle() {
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.i(TAG, "camera is idle");
                // get coordinates and zoom level of current screen bounds
                LatLng southwest = map.getProjection().getVisibleRegion().latLngBounds.southwest;
                LatLng northeast = map.getProjection().getVisibleRegion().latLngBounds.northeast;
                map.getProjection().getVisibleRegion();
                float zoomLevel = map.getCameraPosition().zoom;
                // calculate bounds to get more markers
                getRadius(southwest, northeast, zoomLevel);
            }
        });


    }

    /* Calculates the bounds in which to get the next markers
     * Based on the camera position and zoom level */
    public void getRadius(LatLng southwest, LatLng northeast, float zoomLevel) {
        int numMarkersToGet;
        double southwestLat = southwest.latitude, southwestLong = southwest.longitude;
        double northeastLat = northeast.latitude, northeastLong = northeast.longitude;

        // TODO: refactor this method, change the logic
        if (zoomLevel < 5) {
            numMarkersToGet = 50;
            southwestLat = southwestLat - 5;
            southwestLong = southwestLong - 5;
            northeastLat = northeastLat + 5;
            northeastLong = northeastLong + 5;

        } else if (zoomLevel < 9) {
            numMarkersToGet = 40;
            southwestLat = southwestLat - 4;
            southwestLong = southwestLong - 4;
            northeastLat = northeastLat + 4;
            northeastLong = northeastLong + 4;

        } else if (zoomLevel < 12) {
            numMarkersToGet = 30;
            southwestLat = southwestLat - 3;
            southwestLong = southwestLong - 3;
            northeastLat = northeastLat + 3;
            northeastLong = northeastLong + 3;

        } else if (zoomLevel < 16) {
            numMarkersToGet = 20;
            southwestLat = southwestLat - 2;
            southwestLong = southwestLong - 2;
            northeastLat = northeastLat + 2;
            northeastLong = northeastLong + 2;

        } else {
            numMarkersToGet = 10;
            southwestLat = southwestLat - 1;
            southwestLong = southwestLong - 1;
            northeastLat = northeastLat + 1;
            northeastLong = northeastLong + 1;
        }
        // create the two points needed for the rectangular bound
        ParseGeoPoint southwestBound = new ParseGeoPoint(southwestLat, southwestLong);
        ParseGeoPoint northeastBound = new ParseGeoPoint(northeastLat, northeastLong);
        // get more markers within the bounds
        getMarkers(numMarkersToGet, southwestBound, northeastBound);
    }

    /* Uses Google's Places API to display place name for autocomplete searching */
    public void setupAutocompleteSearch() {
        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), getString(R.string.maps_api_key));

        // Initialize the AutocompleteSupportFragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                moveToSearchedLocation(place);
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    private void moveToSearchedLocation(@NonNull Place place) {
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

}