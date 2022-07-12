package com.example.hidden_treasures.map;

import static com.parse.Parse.getApplicationContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.BitmapFormat;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private MarkerViewModel markerViewModel;

    private List<String> markerIDs = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private List<MarkerEntity> markerEntities = new ArrayList<>();
    private List<Marker> removedMarkers = new ArrayList<>();
    private GoogleMap map;
    private List<Polyline> lines = new ArrayList<>();

    private LatLng lastExploredLocation = null;
    private ParseGeoPoint southwestBound = null;
    private ParseGeoPoint northeastBound = null;
    private float lastZoomLevel = 0;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // associating marker view model with map fragment and getting the marker view model
        markerViewModel = new ViewModelProvider(this).get(MarkerViewModel.class);
        markerViewModel.getAllMarkers().observe(this, markers -> {
            // save markers whenever it updates
            markerEntities.clear();
            markerEntities.addAll(markers);
            Log.i("MarkerLiveData", String.valueOf(markerEntities.size()));
        });

        if (savedInstanceState != null) {
            Log.i(TAG, "getting saved values");
            // get values from last query for markers
            lastExploredLocation = savedInstanceState.getParcelable("lastExploredLocation");
            southwestBound = savedInstanceState.getParcelable("southwestBound");
            northeastBound = savedInstanceState.getParcelable("northeastBound");
            lastZoomLevel = savedInstanceState.getFloat("lastZoomLevel");
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
        Log.i(TAG, "on save instance being called");
        // save values for the last query for markers
        outState.putParcelable("lastExploredLocation", lastExploredLocation);
        outState.putParcelable("southwestBound", southwestBound);
        outState.putParcelable("northeastBound", northeastBound);
        outState.putFloat("lastZoomLevel", lastZoomLevel);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        // set map style
        map.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.map_style_json)));
        //map.setMinZoomPreference(9);
        Log.i(TAG, "showing map");

        // if previous state was restored
        if (lastExploredLocation != null) {
            // go to last looked at location and get those markers again
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastExploredLocation, lastZoomLevel));
            getMarkers(numMarkersToGet(lastZoomLevel), southwestBound, northeastBound);
        } else {
            Log.i(TAG, "default initial position");
            // set initial position of map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.4530, -122.1817), 10));
            /* To get initial markers */
            ParseGeoPoint southwestBound = new ParseGeoPoint(37.4530 - 5, -122.1817 - 5);
            ParseGeoPoint northeastBound = new ParseGeoPoint(37.4530 + 5, -122.1817 + 5);
            //get markers from database and place on map
            getMarkers(100, southwestBound, northeastBound);
        }

        // enables any markers on the map to be clickable
        enableMarkerClicks();
        // listen for whenever camera is idle on map
        watchCameraIdle();
    }

    public int numMarkersToGet(float zoomLevel) {
        if (zoomLevel > 8) {
            return 100;
        } else {
            return 100000;
        }
    }

    /* retrieves markers from database, then calls a function to place markers on map */
    private void getMarkers(int numMarkersToGet, ParseGeoPoint southwestBound, ParseGeoPoint northeastBound) {
        this.southwestBound = southwestBound;
        this.northeastBound = northeastBound;
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
    private void placeMarkersOnMap(List<ParseMarker> parseMarkers) {
        if (parseMarkers != null && parseMarkers.size() > 0) {
            // id is used to get random images
            int id = 1;
            for (ParseMarker marker : parseMarkers) {
                // get the marker values
                LatLng markerLocation = new LatLng(marker.getLocation().getLatitude(), marker.getLocation().getLongitude());
                // TODO: replace random images with the real media url
                // generate random images for markers for now for the test data
                String url = "https://picsum.photos/id/" + id + "/200/300";

                //Bitmap image = getMarkerIcon(marker.getMedia());

                Marker mapMarker = map.addMarker(new MarkerOptions()
                        .position(markerLocation)
                        .title(marker.getTitle()));
                        //.icon(BitmapDescriptorFactory.fromBitmap(image)));
                mapMarker.setTag(url);
                markers.add(mapMarker);
                id++;
            }
            Log.i(TAG, "placed markers on map");
        }
    }

    public Bitmap getMarkerIcon(ParseFile media) {
        Bitmap image = null;
        try {
            image = BitmapFactory.decodeFile(media.getFile().getAbsolutePath());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (image != null) {
            BitmapFormat formatIcon = new BitmapFormat();
            image = Bitmap.createScaledBitmap(image, 75, 75, false);
            image = formatIcon.getCircularBitmap(image);
            image = formatIcon.addBorderToCircularBitmap(image, 4, Color.WHITE);
            image = formatIcon.addShadowToCircularBitmap(image, 4, Color.LTGRAY);
        }
        return image;
    }

    /* adds an individual newly created marker to the cluster manager */
    public void addCreatedMarker(String title, LatLng location, ParseFile media) {
        Bitmap image = getMarkerIcon(media);
        Marker newMarker = map.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(image)));
        newMarker.setTag(media.getUrl());
        Log.i(TAG, "moving camera to new marker");
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.8566, 2.3522), 13));
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

                // create a new marker detail fragment instance and pass in image url, title
                MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance((String) marker.getTag(), marker.getTitle());
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
                lastExploredLocation = map.getCameraPosition().target;
                lastZoomLevel = map.getCameraPosition().zoom;
                // get coordinates and zoom level of current screen bounds
                LatLng southwest = map.getProjection().getVisibleRegion().latLngBounds.southwest;
                LatLng northeast = map.getProjection().getVisibleRegion().latLngBounds.northeast;
                float zoomLevel = map.getCameraPosition().zoom;

                // create the two points needed for the rectangular bound
                ParseGeoPoint southwestBound = new ParseGeoPoint(southwest.latitude, southwest.longitude);
                ParseGeoPoint northeastBound = new ParseGeoPoint(northeast.latitude, northeast.longitude);
                // get more markers within the bounds
                getMarkers(numMarkersToGet(zoomLevel), southwestBound, northeastBound);

                // re-cluster markers
                clusterMarkers();
            }
        });
    }

    public void clusterMarkers() {
        Log.i(TAG, "now onto clustering");
        // first split screen into grid and get an array of 16 cells
        LatLngBounds[] bounds = splitIntoGrid();
        // iterate through each bound
        Log.i(TAG, String.valueOf(bounds.length));
        for (int i = 0; i < 16; i++) {
            //find count of markers within each bound
            int count = 0;
            for (Marker marker : markers) {
                if (bounds[i].contains(marker.getPosition())) {
                    count++;
                    //if count > 5, only show the five markers, add others to list of invisible/removed
                    if (count > 5) {
                        // add removed marker to list
                        removedMarkers.add(marker);
                        marker.setVisible(false);
                    }
                }
            }
            // if there's more space within a cell for more markers, go through the removed markers list
            List<Marker> toBeAddedBack = new ArrayList<>();
            if (count < 5) {
                for (Marker marker : removedMarkers) {
                    if (count < 5) {
                        if (bounds[i].contains(marker.getPosition())) {
                            marker.setVisible(true);

                            // take it out from removedMarkers list later by adding it to an toBeAddedBack list
                            toBeAddedBack.add(marker);
                            count++;
                        }
                    } else {
                        break;
                    }
                }
            }
            // clean up removed markers list
            for (Marker marker: toBeAddedBack) {
                removedMarkers.remove(marker);
            }
            toBeAddedBack.clear();

            // TODO: instead of just removing, show the count of removed on map so user knows there are more
            // remove all the extra markers
            for (Marker marker : removedMarkers) {
                marker.setVisible(false);
            }
        }
    }

    /* Splits the screen into 16 cells, a 4x4 grid */
    public LatLngBounds[] splitIntoGrid() {
        for (Polyline line : lines) {
            line.remove();
        }
        // get the bounds for the screen
        LatLng southwest = map.getProjection().getVisibleRegion().latLngBounds.southwest;
        LatLng northeast = map.getProjection().getVisibleRegion().latLngBounds.northeast;

        // calculate cell height and width
        double totalHeight = Math.abs(northeast.latitude - southwest.latitude);
        double totalWidth = Math.abs(southwest.longitude - northeast.longitude);
        double cellHeight = totalHeight / 4;
        double cellWidth = totalWidth / 4;

        // create a new array to store the 16 cell bounds
        LatLngBounds[] gridCells = new LatLngBounds[16];
        // set the first cell's bounds
        gridCells[0] = new LatLngBounds(southwest, new LatLng(southwest.latitude + cellHeight, southwest.longitude + cellWidth));
        // set the remaining cell bounds
        for (int i = 1; i < 16; i++) {
            if (i % 4 == 0) {
                gridCells[i] = new LatLngBounds(new LatLng(gridCells[i-4].southwest.latitude, gridCells[i-4].southwest.longitude + cellWidth), new LatLng(gridCells[i-4].northeast.latitude, gridCells[i-4].northeast.longitude + cellWidth));
            } else {
                gridCells[i] = new LatLngBounds(new LatLng(gridCells[i-1].southwest.latitude + cellHeight, gridCells[i-1].southwest.longitude), new LatLng(gridCells[i-1].northeast.latitude + cellHeight, gridCells[i-1].northeast.longitude));
            }
            Log.i(TAG, gridCells[i].southwest + " " + gridCells[i].northeast);
        }
        return gridCells;
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