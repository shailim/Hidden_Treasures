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
import com.bumptech.glide.load.resource.gif.GifDrawable;
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
import com.parse.Parse;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private Handler handler;

    private AmazonS3Client s3Client;

    private MarkerViewModel markerViewModel;

    private Set<String> markerIDs = new HashSet<>();
    private List<Marker> markers = new ArrayList<>();
    private List<Marker> removedMarkers = new ArrayList<>();
    private GoogleMap map;

    private LatLng lastExploredLocation = null;
    private float lastZoomLevel = 0;
    private float lastZoomIn = 0;
    private LatLngBounds lastExploredBounds = null;

    private HashMap<String, HashMap<String, List<Marker>>> markerTable = new HashMap<>();
    private List<Marker> clusters = new ArrayList<>();


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
            // go to last looked at location and get those markers again
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastExploredLocation, lastZoomLevel));
        } else {
            // set initial position of map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.4530, -122.1817), 13));
        }
        lastExploredBounds = map.getProjection().getVisibleRegion().latLngBounds;
        // get an initial set of markers
        getMarkersFromCache(numMarkersToGet(map.getCameraPosition().zoom), map.getProjection().getVisibleRegion().latLngBounds);
        // enables any markers on the map to be clickable
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


    /* retrieves markers from database, then calls a function to place com.example.hidden_treasures.markers on map */
    private void getMarkersFromCache(int numMarkersToGet, LatLngBounds bound) {
        // get markers from cache in background and after completion, place on map
        FutureTask<List<MarkerEntity>> futureTask = new FutureTask<>(() -> markerViewModel.getWithinBounds(bound.southwest.latitude,
                bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude, numMarkersToGet));
        AppDatabase.databaseWriteExecutor.execute(futureTask);

        // check if the task completed, otherwise wait and check again
        while(!futureTask.isDone() && !futureTask.isCancelled()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            List<MarkerEntity> markers = futureTask.get();
            Log.i(TAG, "list size: " + markers.size());
            // display on map
            placeMarkerEntitiesOnMap(markers);
            // update the last accessed time for the markers from cache
            updateLastAccessed(markers);
            // then get more markers from server
            getMarkersFromServer(numMarkersToGet, bound, markerIDs);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /* retrieves markers from Parse database */
    private void getMarkersFromServer(int numMarkersToGet, LatLngBounds bound, Set<String> ids) {
        ParseQuery<ParseMarker> query = ParseQuery.getQuery(ParseMarker.class);
        query.whereNotContainedIn("roomId", ids);
        query.setLimit(numMarkersToGet);
        ParseGeoPoint southwest = new ParseGeoPoint(bound.southwest.latitude, bound.southwest.longitude);
        ParseGeoPoint northeast = new ParseGeoPoint(bound.northeast.latitude, bound.northeast.longitude);
        query.whereWithinGeoBox("location", southwest, northeast);
        query.findInBackground(new FindCallback<ParseMarker>() {
            @Override
            public void done(List<ParseMarker> objects, ParseException e) {
                if (e == null) {
                    // place onto map, again checking ids for repeats
                    placeParseMarkersOnMap(objects);
                    // remove markers in this area from cache
                    markerViewModel.delete(bound.southwest.latitude, bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude);
                    // store these new ones into cache
                    storeNewMarkers(objects);
                }
            }
        });
    }

    // updates the last accessed time for the each marker retrieved
    private void updateLastAccessed(List<MarkerEntity> markers ) {
        for (MarkerEntity marker : markers) {
            markerViewModel.updateLastAccessed(marker.objectId);
        }
    }

    /* Stores newly retrieved markers into cache */
    private void storeNewMarkers(List<ParseMarker> objects) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (ParseMarker object : objects) {
                    String title = object.getTitle();
                    String id = object.getRoomid();
                    long time = object.getTime();
                    double latitude = object.getLocation().getLatitude();
                    double longitude = object.getLocation().getLongitude();
                    String imageKey = object.getImage();
                    String createdBy = object.getCreatedBy();
                    int viewCount = object.getViewCount();
                    int score = object.getScore();
                    MarkerEntity marker = new MarkerEntity(id, time, title, latitude, longitude, imageKey, createdBy, viewCount, score);
                    markerViewModel.insertMarker(marker);
            }
        });
    }

    /* Using MarkerEntity instead of ParseMarker, Creates new markers and places them on the map */
    private void placeMarkerEntitiesOnMap(List<MarkerEntity> objects) {
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
                        Bitmap icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        mapMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
                        Log.i(TAG, "icon already created");
                } else {
                    setMarkerIcon(mapMarker, object.imageKey, object.objectId);
                }
                markers.add(mapMarker);
                addToMarkerTable(mapMarker);
            }
            clusterMarkers();
        }
    }

    /* Using MarkerEntity instead of ParseMarker, Creates new com.example.hidden_treasures.markers and places them on the map */
    private void placeParseMarkersOnMap(List<ParseMarker> objects) {
        if (objects != null && objects.size() > 0) {
            // id is used to get random images
            for (ParseMarker object : objects) {
                if (markerIDs.contains(object.getRoomid())) {
                    continue;
                }
                // add ID of each marker placed on map to the list
                markerIDs.add(object.getRoomid());
                // get the marker values
                LatLng markerLocation = new LatLng(object.getLocation().getLatitude(), object.getLocation().getLongitude());

                Marker mapMarker = map.addMarker(new MarkerOptions()
                        .position(markerLocation)
                        .title(object.getTitle()));

                // create an object to store marker data values
                MarkerData data = new MarkerData(object.getRoomid(), object.getViewCount(), new Date(object.getTime()), object.getImage());
                mapMarker.setTag(data);
                if (object.getIcon() != null) {
                    byte[] bytes = object.getIcon();
                    Bitmap icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    mapMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
                    Log.i(TAG, "icon already created");
                } else {
                    setMarkerIcon(mapMarker, object.getImage(), object.getRoomid());
                }
                markers.add(mapMarker);
                addToMarkerTable(mapMarker);
            }
        }
    }

    // according to the geohash of the marker, stores it into the marker table
    private void addToMarkerTable(Marker marker) {
        // get geohash of marker
        // TODO: when uploading markers, generate their geohash, for testing I'm just generating here
        String hash = GeoHash.encode(marker.getPosition().latitude, marker.getPosition().longitude, 7);
        // if an entry for the geohash prefix of 4 characters doesn't exist
        if (markerTable.get(hash.substring(0, 2)) == null) {
            HashMap<String, List<Marker>> newTable = new HashMap<>();
            List<Marker> list = new ArrayList<>();
            list.add(marker);
            // create a new hash table with a list value
            newTable.put(hash.substring(0, 4), list);
            markerTable.put(hash.substring(0, 2), newTable);
            Log.i(TAG, "added new entry to first table");
            // if an entry in the inner table for geohashes of precision length 7 doesn't exist
        } else if (markerTable.get(hash.substring(0, 2)).get(hash.substring(0, 4)) == null) {
            List<Marker> list = new ArrayList<>();
            list.add(marker);
            // create a new entry with a list value
            markerTable.get(hash.substring(0, 2)).put(hash.substring(0, 4), list);
            Log.i(TAG, "added new entry to second table");
        } else {
            // add the marker to the list
            markerTable.get(hash.substring(0, 2)).get(hash.substring(0, 4)).add(marker);
            Log.i(TAG, "added to list in marker table: " + hash);
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
                if (marker.getTag() != null && marker.getTag() instanceof MarkerData) {
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
                } else if (marker.getTag() instanceof ArrayList) {
                    List<Marker> markers = (ArrayList) marker.getTag();
                    FragmentManager childFragMan = getChildFragmentManager();
                    FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                    // create a new marker detail fragment instance and pass in image url, title
                    MultipleMarkerDetailFragment multipleMarkerFrag = MultipleMarkerDetailFragment.newInstance(markers);
                    // add the child fragment to current map fragment
                    childFragTrans.add(R.id.mapFragmentLayout, multipleMarkerFrag);
                    childFragTrans.commit();
                }
                return false;
            }
        });
    }

    int i = 0;
    Runnable r = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "camera is idle: " + i);
            // get current camera position
            lastExploredLocation = map.getCameraPosition().target;
            lastZoomLevel = map.getCameraPosition().zoom;

            if (lastZoomLevel > 11) {
                deCluster();
            }
            // when camera idle and camera out of previous bounds or zoomed in more than twice or zoomed out more than twice
            if (lastZoomIn - map.getCameraPosition().zoom < -2 || lastZoomIn - map.getCameraPosition().zoom > 2) {
                lastZoomIn = map.getCameraPosition().zoom;
                updateMap();
                return;
            } else if (!lastExploredBounds.contains(lastExploredLocation)) {
                updateMap();
            } else if (lastZoomLevel <= 11) {
                clusterMarkers();
            }
        }
    };

    // calculates new area to retrieve markers and queries for markers
    private void updateMap() {
        // get the area around screen, calculate height and width of screen lat/lng
        LatLngBounds curBound = map.getProjection().getVisibleRegion().latLngBounds;
        lastExploredBounds = curBound;
        double cellHeight = Math.abs(curBound.northeast.latitude - curBound.southwest.latitude);
        double cellWidth = Math.abs(curBound.northeast.longitude - curBound.southwest.longitude);
        // get the bounds for the whole area
        LatLng northeast = new LatLng(curBound.northeast.latitude + cellHeight, curBound.northeast.longitude + cellWidth);
        LatLng southwest = new LatLng(curBound.southwest.latitude - cellHeight, curBound.southwest.longitude - cellWidth);
        LatLngBounds outerBound = new LatLngBounds(southwest, northeast);
        // query local with the two bound points (if zoom is < 11, get 50, else get all)
        getMarkersFromCache(numMarkersToGet(lastZoomLevel), outerBound);
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
                handler.postDelayed(r, 500);
            }
        });
    }

    public void clusterMarkers() {
            Log.i(TAG, "now onto clustering");
            String curHash = GeoHash.encode(map.getCameraPosition().target.latitude, map.getCameraPosition().target.longitude, 2);
            if (markerTable.get(curHash) != null) {
                for (Map.Entry<String, List<Marker>> set : markerTable.get(curHash).entrySet()) {
                    if (set.getValue() != null && set.getValue().size() > 2) {
                        List<Marker> removed = new ArrayList<>();
                        for (Marker marker : set.getValue()) {
                            marker.setVisible(false);
                            removed.add(marker);
                            Log.i(TAG, "removed a marker");
                        }
                        // TODO: get the center point of the geohash bound and set cluster position to that
                        Marker cluster = map.addMarker(new MarkerOptions().title("Cluster").position(set.getValue().get(0).getPosition()));
                        setCLusterIcon(cluster);
                        cluster.setTag(removed);
                        clusters.add(cluster);
                    }
                }
            }
    }

    private void setCLusterIcon(Marker cluster) {
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.pin_removebg_preview);
        image = Bitmap.createScaledBitmap(image, 100, 100, false);
        image = BitmapFormat.getCircularBitmap(image);
        cluster.setIcon(BitmapDescriptorFactory.fromBitmap(image));
    }

    // removes clusters and re-shows markers
    private void deCluster() {
        for (Marker cluster : clusters) {
            // re-show all the markers
            for (Marker marker : (List<Marker>) cluster.getTag()) {
                marker.setVisible(true);
            }
            // remove the cluster
            cluster.remove();
        }
        // empty the clusters list
        clusters.clear();
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