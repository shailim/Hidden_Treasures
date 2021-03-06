package com.example.hidden_treasures.map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.markers.MarkerData;
import com.example.hidden_treasures.markers.MarkerDetailFragment;
import com.example.hidden_treasures.markers.ParseMarker;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.BitmapFormat;
import com.example.hidden_treasures.util.S3Helper;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = "MapFragment";

    private Handler handler;


    private MarkerViewModel markerViewModel;

    private Set<String> markerIDs = new HashSet<>();
    private List<Marker> markers = new ArrayList<>();
    protected GoogleMap map;

    private LatLng lastExploredLocation = null;
    private float lastZoomLevel = 0;
    private float lastZoomIn = 0;
    private LatLngBounds lastExploredBounds = null;

    protected HashMap<String, HashMap<String, List<Marker>>> markerTable = new HashMap<>();
    protected List<Marker> clusters = new ArrayList<>();


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
        if (savedInstanceState != null) {
            lastExploredLocation = savedInstanceState.getParcelable("lastExploredLocation");
            lastZoomLevel = savedInstanceState.getFloat("lastZoomLevel");
        }
        // associating marker view model with map fragment and getting the marker view model
        markerViewModel = new ViewModelProvider(this).get(MarkerViewModel.class);

        markerViewModel.getAllMarkers().observe(this, markers -> {
            //display on map
            placeMarkerEntitiesOnMap(markers);
        });
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
        MapSearch.setupAutocompleteSearch(this);

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
        // enables any markers on the map to be clickable
        enableMarkerClicks();
        // listen for whenever camera is idle on map
        watchCameraIdle();
    }

    // updates marker's view count when clicked on
    public void updateViewCount(String id, int num) {
        markerViewModel.updateViewCount(id, num);
    }

    /* retrieves markers from database, then calls a function to place com.example.hidden_treasures.markers on map */
    private void getMarkersFromCache(int numMarkersToGet, LatLngBounds bound) {
        markerViewModel.getFromCache(bound.southwest.latitude, bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude, numMarkersToGet);
    }

    /* Using MarkerEntity instead of ParseMarker, Creates new markers and places them on the map */
    private void placeMarkerEntitiesOnMap(List<MarkerEntity> objects) {
        if (objects != null && objects.size() > 0) {
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
                } else {
                    setMarkerIcon(mapMarker, object.imageKey, object.objectId);
                }
                markers.add(mapMarker);
                addToMarkerTable(mapMarker);
            }
        }
    }

    // according to the geohash of the marker, stores it into the marker table
    private void addToMarkerTable(Marker marker) {
        markerTable = MapHelper.addToMarkerTable(markerTable, marker);
    }

    public long sd_card_free(){
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availBlocks = stat.getAvailableBlocksLong();
        long blockSize = stat.getBlockSizeLong();
        long free_memory = availBlocks * blockSize;

        return free_memory;
    }

    // sets the image icon for the marker
    public void setMarkerIcon(Marker marker, String imageKey, String id) {
        URL url = S3Helper.getSignedUrl(getContext(), imageKey);
        Glide.with(this).asBitmap().load(url.toString()).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                resource = Bitmap.createScaledBitmap(resource, 75, 75, false);
                resource = BitmapFormat.getCircularBitmap(resource);
                resource = BitmapFormat.addBorderToCircularBitmap(resource, 4, Color.WHITE);
                resource = BitmapFormat.addShadowToCircularBitmap(resource, 4, Color.LTGRAY);
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource));

                Log.i(TAG, "free bytes: " + sd_card_free());
                // save the bitmap icon to the database
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    resource.compress(Bitmap.CompressFormat.PNG, 0, stream);
                    byte[] bytes = stream.toByteArray();
                    markerViewModel.setIcon(bytes, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // freeing the bitmap object
                resource.recycle();

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
        setMarkerIcon(newMarker, marker.getImage(), marker.getObjectId());
        MarkerData data = new MarkerData(marker.getObjectId(), marker.getViewCount(), new Date(marker.getTime()), marker.getImage());
        newMarker.setTag(data);
        markerIDs.add(marker.getObjectId());
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
                    updateViewCount(data.getId(), data.getViewCount()+1);
                    // show marker detail view
                    openMarkerDetail(data.getId(), data.getImageKey(), marker.getTitle(), data.getViewCount(), data.getDate(), new ArrayList<>());
                } else if (marker.getTag() instanceof ArrayList) {
                    List<Marker> markers = (ArrayList) marker.getTag();
                    // show marker detail view
                    openMarkerDetail(null, null, null, 0, null, markers);
                }
                return false;
            }
        });
    }

    // sets the child fragment of MarkerDetailView
    public void openMarkerDetail(String id, String imageKey, String title, int viewCount, Date date, List<Marker> markers) {
        // to set marker detail as a child fragment
        FragmentManager childFragMan = getChildFragmentManager();
        FragmentTransaction childFragTrans = childFragMan.beginTransaction();

        // create a new marker detail fragment instance and pass in image url, title
        MarkerDetailFragment markerDetailFragment = MarkerDetailFragment.newInstance(id, imageKey, title, viewCount, date, markers);
        // add the child fragment to current map fragment
        childFragTrans.add(R.id.mapFragmentLayout, markerDetailFragment);
        childFragTrans.addToBackStack(null);
        childFragTrans.commit();
    }

    // runs whenever camera is idle on map
    Runnable cameraIdle = new Runnable() {
        @Override
        public void run() {
            // get current camera position
            lastExploredLocation = map.getCameraPosition().target;
            LatLngBounds curBound = lastExploredBounds;
            // when camera idle and camera out of previous bounds or zoomed in more than twice or zoomed out more than twice
            if (lastZoomIn - map.getCameraPosition().zoom < -2 || lastZoomIn - map.getCameraPosition().zoom > 2) {
                lastZoomIn = map.getCameraPosition().zoom;
                updateMap();
                return;
            } else if (!lastExploredBounds.contains(lastExploredLocation)) {
                updateMap();
                // if moving to another location, de-cluster markers in previous location
                MapCluster.deCluster(MapFragment.this);
            }
            //check whether to cluster or de-cluster markers
            if (!curBound.contains(lastExploredLocation) && map.getCameraPosition().zoom <= 11) {
                // if in a different location and zoomed out, cluster markers
                MapCluster.clusterMarkers(MapFragment.this);
            } else if (map.getCameraPosition().zoom <= 11 && lastZoomLevel > 11) {
                // if zoom changed from zoomed in to out, cluster markers
                MapCluster.clusterMarkers(MapFragment.this);
            } else if (map.getCameraPosition().zoom > 11 && lastZoomLevel <= 11) {
                // if zoom changed from zoomed out to in, de-cluster markers
                MapCluster.deCluster(MapFragment.this);
            }
            lastZoomLevel = map.getCameraPosition().zoom;
        }
    };

    // calculates new area to retrieve markers and queries for markers
    private void updateMap() {
        lastExploredBounds = map.getProjection().getVisibleRegion().latLngBounds;
        // get the area around screen, calculate height and width of screen lat/lng
        // then query local with the two bound points (if zoom is < 11, get 50, else get all)
        getMarkersFromCache(MapHelper.numMarkersToGet(lastZoomLevel), MapHelper.getOuterBound(lastExploredBounds));
    }

    /* Adds a listener to track when camera is idle on map */
    public void watchCameraIdle() {
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                // remove runnable from the queue (hasn't been started yet)
                handler.removeCallbacks(cameraIdle);
                // add runnable to queue and run only after the set amount of time
                handler.postDelayed(cameraIdle, 500);
            }
        });
    }
}