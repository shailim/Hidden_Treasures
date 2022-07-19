package com.example.hidden_treasures.map;

import static com.parse.Parse.getApplicationContext;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.hidden_treasures.MarkerRoomDB.AppDatabase;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.markers.MarkerData;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.BitmapFormat;
import com.example.hidden_treasures.util.GeoHash;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private Handler handler;

    private AmazonS3Client s3Client;

    private MarkerViewModel markerViewModel;

    private Set<String> markerIDs = new HashSet<>();
    private List<Marker> markers = new ArrayList<>();
    private List<MarkerEntity> markerEntities = new ArrayList<>();
    private List<Marker> removedMarkers = new ArrayList<>();
    private GoogleMap map;

    private LatLng lastExploredLocation = null;
    private float lastZoomLevel = 0;
    private float lastZoomIn = 0;

    private String curGeohash;
    private String[] adjacentGeohash;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        BasicAWSCredentials credentials = new BasicAWSCredentials(getString(R.string.aws_accessID), getString(R.string.aws_secret_key));
        s3Client = new AmazonS3Client(credentials);
        if (savedInstanceState != null) {
            lastExploredLocation = savedInstanceState.getParcelable("lastExploredLocation");
            lastZoomLevel = savedInstanceState.getFloat("lastZoomLevel");
        }
        // associating marker view model with map fragment and getting the marker view model
        markerViewModel = new ViewModelProvider(this).get(MarkerViewModel.class);
        // get the initial set of com.example.hidden_treasures.markers from view model
        markerViewModel.getWithinBounds().observe(this, markers -> {
            markerEntities.clear();
            markerEntities.addAll(markers);
        });

        // updating score of all markers
        LiveData<List<MarkerEntity>> liveData;
        liveData = markerViewModel.getAllMarkers();
        liveData.observe(this, marker -> {
            markerViewModel.updateScore();
            // after updating the score, remove the observer so it doesn't keep updating
            liveData.removeObservers(this);
        });

        // getting the initial position's geohash and adjacent cells
        curGeohash = GeoHash.encode(37.4530, -122.1817, 6);
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
        // save values for the last query for com.example.hidden_treasures.markers
        outState.putParcelable("lastExploredLocation", lastExploredLocation);
        outState.putFloat("lastZoomLevel", lastZoomLevel);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        // set map style
        map.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.map_style_json)));
        map.setMinZoomPreference(9);
        Log.i(TAG, "showing map");

        // if previous state was restored
        if (lastExploredLocation != null) {
            // go to last looked at location and get those com.example.hidden_treasures.markers again
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastExploredLocation, lastZoomLevel));
        } else {
            // set initial position of map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.4530, -122.1817), 13));
        }
        // place initial set of com.example.hidden_treasures.markers on map
        placeMarkersOnMap(markerEntities);
        // getting the com.example.hidden_treasures.markers from the eight cells around
        adjacentGeohash = GeoHash.neighbours(curGeohash);
        for (String cell : adjacentGeohash) {
            LatLngBounds bound = GeoHash.bounds(cell);
            markerViewModel.getWithinBounds(bound.southwest.latitude, bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude, 50).observe(this, markers -> {
                placeMarkersOnMap(markers);
            });
        }
        // enables any com.example.hidden_treasures.markers on the map to be clickable
        enableMarkerClicks();
        // listen for whenever camera is idle on map
        watchCameraIdle();
    }

    public int numMarkersToGet(float zoomLevel) {
        if (zoomLevel < 11) {
            return 50;
        } else {
            return 10000;
        }
    }

    /* Determines how precise the geohash should be, how much area it should cover */
    public int geohashPrecision(float zoomLevel) {
        int zoom = Math.round(zoomLevel);
        switch (zoom) {
            case 9:
            case 10:
                return 4;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return 5;
            case 16:
            case 17:
                return 6;
            default:
                return 4;
        }
    }


    /* retrieves com.example.hidden_treasures.markers from database, then calls a function to place com.example.hidden_treasures.markers on map */
    private void getMarkers(int numMarkersToGet, LatLngBounds bound) {
        markerViewModel.getWithinBounds(bound.southwest.latitude,
                        bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude, numMarkersToGet).observe(MapFragment.this, markers -> {
                    placeMarkersOnMap(markers);
                });
    }

    /* Using MarkerEntity instead of ParseMarker, Creates new com.example.hidden_treasures.markers and places them on the map */
    private void placeMarkersOnMap(List<MarkerEntity> objects) {
        if (objects != null && objects.size() > 0) {
            // id is used to get random images
            for (MarkerEntity object : objects) {
                if (markerIDs.contains(object.objectId)) {
                    continue;
                }
                // add ID of each marker placed on map to the list
                markerIDs.add(object.objectId);
                // get the marker values
                LatLng markerLocation = new LatLng(object.latitude, object.longitude);

                Marker mapMarker = map.addMarker(new MarkerOptions()
                        .position(markerLocation)
                        .title(object.title));

                // create an object to store marker data values
                MarkerData data = new MarkerData(object.objectId, object.view_count, new Date(object.createdAt), object.imageKey);
                mapMarker.setTag(data);
                if (object.icon != null) {
                        byte[] bytes = object.icon;
                        //byte[] bytes = object.icon.getBytes(1, (int)object.icon.length());
                        Bitmap icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        mapMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
                        Log.i(TAG, "icon already created");
                } else {
                    setMarkerIcon(mapMarker, object.imageKey, object.objectId);
                }
                markers.add(mapMarker);
            }
            //clusterMarkers();
        }
    }

    // generates a signed url to access the image in s3
    private URL getSignedUrl(String key) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(getString(R.string.s3_bucket), key)
                        .withMethod(HttpMethod.GET);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }

    // sets the image icon for the marker
    public void setMarkerIcon(Marker marker, String imageKey, String id) {
        URL url = getSignedUrl(imageKey);
        Glide.with(this).asBitmap().load(url.toString()).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                resource = Bitmap.createScaledBitmap(resource, 75, 75, false);
                resource = BitmapFormat.getCircularBitmap(resource);
                resource = BitmapFormat.addBorderToCircularBitmap(resource, 4, Color.WHITE);
                resource = BitmapFormat.addShadowToCircularBitmap(resource, 4, Color.LTGRAY);
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource));

                // save the bitmap icon to the database
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                resource.compress(Bitmap.CompressFormat.PNG, 0, stream);
                byte[] bytes = stream.toByteArray();
                markerViewModel.setIcon(bytes, id);
            }
            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });
    }

    /* adds an individual newly created marker to the cluster manager */
    public void addCreatedMarker(ParseMarker marker, LatLng location) {
        Marker newMarker = map.addMarker(new MarkerOptions()
                .position(location)
                .title(marker.getTitle()));
        MarkerData data = new MarkerData(marker.getRoomid(), marker.getViewCount(), new Date(marker.getTime()), marker.getImage());
        newMarker.setTag(data);
        setMarkerIcon(newMarker, marker.getImage(), marker.getRoomid());
        Log.i(TAG, "moving camera to new marker");
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
    }

    /* Adds a click listener on com.example.hidden_treasures.markers
     * On marker click, a marker detail view opens up  */
    @SuppressLint("PotentialBehaviorOverride")
    public void enableMarkerClicks() {
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                MarkerData data = (MarkerData) marker.getTag();
                markerViewModel.updateViewCount(data.getId(), data.getViewCount());
                // to set marker detail as a child fragment
                FragmentManager childFragMan = getChildFragmentManager();
                FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                // create a new marker detail fragment instance and pass in image url, title
                MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance(data.getImageKey(), marker.getTitle(), data.getViewCount(), data.getDate());
                // add the child fragment to current map fragment
                childFragTrans.add(R.id.mapFragmentLayout, markerDetailFrag);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
                return false;
            }
        });
    }

    int i = 0;
    Runnable r = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "camera is idle: " + i);
            lastExploredLocation = map.getCameraPosition().target;
            lastZoomLevel = map.getCameraPosition().zoom;

            // whether user is in the same area or not, if they zoomed in or out more than two levels, get new com.example.hidden_treasures.markers
            if (lastZoomIn - map.getCameraPosition().zoom < -2 || lastZoomIn - map.getCameraPosition().zoom > 2) {
                lastZoomIn = map.getCameraPosition().zoom;
                updateAllNineCells();
                return;
            }

            // if the camera position is outside the current center bound but
            // if marker latlng is inside the bounds of a neighbor, do something different for each one
            if (!GeoHash.bounds(curGeohash).contains(lastExploredLocation)) {
                boolean stillWithinBounds = false;
                for (int i = 0; i < adjacentGeohash.length-1; i++) {
                    if (GeoHash.bounds(adjacentGeohash[i]).contains(lastExploredLocation)) {
                        stillWithinBounds = true;
                        int numMarkers = numMarkersToGet(lastZoomLevel);
                        // update current geohash cell
                        curGeohash = adjacentGeohash[i];
                        // update the adjacent list
                        adjacentGeohash = GeoHash.neighbours(curGeohash);
                        updateSomeCells(numMarkers, adjacentGeohash, i);
                    }
                }
                if (!stillWithinBounds) {
                    updateAllNineCells();
                }
            }
        }
    };

    /* recalculates current and adjacent cells and gets com.example.hidden_treasures.markers for all of them */
    private void updateAllNineCells() {
        curGeohash = GeoHash.encode(map.getCameraPosition().target.latitude, map.getCameraPosition().target.longitude, geohashPrecision(lastZoomLevel));
        int numMarkers = numMarkersToGet(lastZoomLevel);
        getMarkers(numMarkers, GeoHash.bounds(curGeohash));
        adjacentGeohash = GeoHash.neighbours(curGeohash);
        for (String cell : adjacentGeohash) {
            getMarkers(numMarkers, GeoHash.bounds(cell));
        }
        getMarkers(numMarkersToGet(map.getCameraPosition().zoom), GeoHash.bounds(curGeohash));
    }

    /* shifts current cell to a neighbor cell position, gets new com.example.hidden_treasures.markers based on which neighbor cell the user is in */
    private void updateSomeCells(int numMarkers, String[] adjacentGeohash, int i) {
        switch (i) {
            case 0:
                // n: get the three cells above it
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[7])); // nw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[0])); // n
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[1])); // ne
                break;
            case 1:
                // ne: get the five cells around it
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[7])); // nw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[0])); // n
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[1])); // ne
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[2])); // e
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[3])); // se
                break;
            case 2:
                // e: get the three cells to the right
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[1])); // ne
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[2])); // e
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[3])); // se
                break;
            case 3:
                // se: get the five cells around
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[1])); // ne
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[2])); // e
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[3])); // se
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[4])); // s
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[5])); // sw
                break;
            case 4:
                // s: get the three below
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[3])); // se
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[4])); // s
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[5])); // sw
                break;
            case 5:
                // sw: get the five around
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[3])); // se
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[4])); // s
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[5])); // sw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[6])); // w
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[7])); // nw
                break;
            case 6:
                // w: get the three cells to the left
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[5])); // sw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[6])); // w
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[7])); // nw
                break;
            case 7:
                // nw: get the five cells around
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[5])); // sw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[6])); // w
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[7])); // nw
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[0])); // n
                getMarkers(numMarkers, GeoHash.bounds(adjacentGeohash[1])); // ne
                break;
        }
    }

    /* Adds a listener to track when camera is idle on map */
    public void watchCameraIdle() {
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                i++;
                // remove runnable from the queue (hasn't been started yet)
                handler.removeCallbacks(r);
                // add runnable to queue and run only after the set amount of time
                handler.postDelayed(r, 1000);
            }
        });
    }

    public void clusterMarkers() {
        Log.i(TAG, "now onto clustering");
        // first split screen into grid and get an array of 16 cells
        LatLngBounds[] bounds = splitIntoGrid();
        // iterate through each bound
        for (int i = 0; i < 16; i++) {
            //find count of com.example.hidden_treasures.markers within each bound
            int count = 0;
            for (Marker marker : markers) {
                if (bounds[i].contains(marker.getPosition())) {
                    count++;
                    //if count > 5, only show the five com.example.hidden_treasures.markers, add others to list of invisible/removed
                    if (count > 3) {
                        // add removed marker to list
                        removedMarkers.add(marker);
                        Log.i(TAG, "removed a marker");
                    }
                }
            }
            // if there's more space within a cell for more com.example.hidden_treasures.markers, go through the removed com.example.hidden_treasures.markers list
            List<Marker> toBeAddedBack = new ArrayList<>();
            if (count < 3) {
                for (Marker marker : removedMarkers) {
                    if (count < 3) {
                        if (bounds[i].contains(marker.getPosition())) {
                            marker.setVisible(true);
                            markers.add(marker);

                            // take it out from removedMarkers list later by adding it to an toBeAddedBack list
                            toBeAddedBack.add(marker);
                            count++;
                        }
                    } else {
                        break;
                    }
                }
            }
            // clean up removed com.example.hidden_treasures.markers list
            for (Marker marker: toBeAddedBack) {
                removedMarkers.remove(marker);
            }
            toBeAddedBack.clear();

            // remove all the extra com.example.hidden_treasures.markers
            for (Marker marker : removedMarkers) {
                marker.setVisible(false);
                markers.remove(marker);
            }
        }
    }

    /* Splits the screen into 16 cells, a 4x4 grid */
    public LatLngBounds[] splitIntoGrid() {
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