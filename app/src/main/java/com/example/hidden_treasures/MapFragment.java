package com.example.hidden_treasures;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap;
import static com.parse.Parse.getApplicationContext;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import com.parse.Parse;
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
    private Location createdMarkerLocation;
    private String createdMarkerMediaUrl;

    private List<String> markerIDs = new ArrayList<>();

    private GoogleMap map;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    public static MapFragment newInstance(String title, Location location, String mediaUrl) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ParseMarker.TITLE, title);
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
        // initial position should show where the user was previously looking
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(prevLatitude, prevLongitude), zoomLevel));

        ParseGeoPoint southwestBound = new ParseGeoPoint(prevLatitude - 5, prevLongitude - 5);
        ParseGeoPoint northeastBound = new ParseGeoPoint(prevLatitude + 5, prevLongitude + 5);
        //get markers from database and place on map
        getMarkers(50, southwestBound, northeastBound);
        // enables any markers on the map to be clickable
        enableMarkerClicks();
        // keep track of camera position on map
        trackCameraPosition();
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
            int id = 1;
            for (ParseMarker marker : markers) {
                // get the marker values
                LatLng userLocation = new LatLng(marker.getLocation().getLatitude(), marker.getLocation().getLongitude());
                String title = marker.getTitle();
                ParseFile media = marker.getMedia();
                Bitmap image = null;
                try {
                    image = BitmapFactory.decodeFile(media.getFile().getAbsolutePath());
                    // change height and width as zoom level changes
                    int height;
                    int width;
                    // resize image for marker icon
                    image = Bitmap.createScaledBitmap(image, 50, 50, false);
                    // Create a circular bitmap
                    image = getCircularBitmap(image);
                    // Add a border around circular bitmap
                    image = addBorderToCircularBitmap(image, 5, Color.WHITE);
                    // Add a shadow around circular bitmap
                    image = addShadowToCircularBitmap(image, 4, Color.LTGRAY);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //create a new marker object with the values
                Marker createdMarker = map.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title(title)
                        .icon(BitmapDescriptorFactory.fromBitmap(image)));
                // set the tag as the image url
                String url = "https://picsum.photos/id/" + id + "/200/300";
                createdMarker.setTag(url);
                id++;
            }
            Log.i(TAG, "placed markers on map");
            // .icon(BitmapDescriptorFactory.fromBitmap(image))
        }
    }

    public void addCreatedMarker(String title, Location location, String imageUrl) {
        if (title != null) {
            //LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            LatLng userLocation = new LatLng(48.8566, 2.3522);
            Marker createdMarker = map.addMarker(new MarkerOptions()
                    .position(userLocation)
                    .title(title));
            // set the tag as the image url
            createdMarker.setTag(imageUrl);
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
                MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance((String) marker.getTag(), marker.getTitle());
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

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.i(TAG, "camera is idle");
                // get coordinates and zoom level of current camera position
                double latitude = map.getCameraPosition().target.latitude;
                double longitude = map.getCameraPosition().target.longitude;
                map.getProjection().getVisibleRegion();
                float zoomLevel = map.getCameraPosition().zoom;
                // calculate bounds to get more markers
                getRadius(latitude, longitude, zoomLevel);
            }
        });


    }

    /* Calculates the bounds in which to get the next markers
     * Based on the camera position and zoom level */
    public void getRadius(double latitude, double longitude, float zoomLevel) {
        int numMarkersToGet;
        double southwestLat, southwestLong, northeastLat, northeastLong;

        if (zoomLevel < 5) {
            numMarkersToGet = 100;
            southwestLat = latitude - 10;
            southwestLong = longitude - 10;
            northeastLat = latitude + 10;
            northeastLong = longitude + 10;

        } else if (zoomLevel < 9) {
            numMarkersToGet = 50;
            southwestLat = latitude - 7;
            southwestLong = longitude - 7;
            northeastLat = latitude + 7;
            northeastLong = longitude + 7;

        } else if (zoomLevel < 12) {
            numMarkersToGet = 25;
            southwestLat = latitude - 5;
            southwestLong = longitude - 5;
            northeastLat = latitude + 5;
            northeastLong = longitude + 5;

        } else if (zoomLevel < 16) {
            numMarkersToGet = 10;
            southwestLat = latitude - 3;
            southwestLong = longitude - 3;
            northeastLat = latitude + 3;
            northeastLong = longitude + 3;

        } else {
            numMarkersToGet = 5;
            southwestLat = latitude - 2;
            southwestLong = longitude - 2;
            northeastLat = latitude + 2;
            northeastLong = longitude + 2;
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

    protected Bitmap getCircularBitmap(Bitmap srcBitmap) {
        // Calculate the circular bitmap width with border
        int squareBitmapWidth = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight());
        // Initialize a new instance of Bitmap
        Bitmap dstBitmap = Bitmap.createBitmap (
                squareBitmapWidth, // Width
                squareBitmapWidth, // Height
                Bitmap.Config.ARGB_8888 // Config
        );
        Canvas canvas = new Canvas(dstBitmap);
        // Initialize a new Paint instance
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, squareBitmapWidth, squareBitmapWidth);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Calculate the left and top of copied bitmap
        float left = (squareBitmapWidth-srcBitmap.getWidth())/2;
        float top = (squareBitmapWidth-srcBitmap.getHeight())/2;
        canvas.drawBitmap(srcBitmap, left, top, paint);
        // Free the native object associated with this bitmap.
        srcBitmap.recycle();
        // Return the circular bitmap
        return dstBitmap;
    }
    // Custom method to add a border around circular bitmap
    protected Bitmap addBorderToCircularBitmap(Bitmap srcBitmap, int borderWidth, int borderColor) {
        // Calculate the circular bitmap width with border
        int dstBitmapWidth = srcBitmap.getWidth()+borderWidth*2;
        // Initialize a new Bitmap to make it bordered circular bitmap
        Bitmap dstBitmap = Bitmap.createBitmap(dstBitmapWidth,dstBitmapWidth, Bitmap.Config.ARGB_8888);
        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(dstBitmap);
        // Draw source bitmap to canvas
        canvas.drawBitmap(srcBitmap, borderWidth, borderWidth, null);
        // Initialize a new Paint instance to draw border
        Paint paint = new Paint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        paint.setAntiAlias(true);
        canvas.drawCircle(
                canvas.getWidth() / 2, // cx
                canvas.getWidth() / 2, // cy
                canvas.getWidth()/2 - borderWidth / 2, // Radius
                paint // Paint
        );
        // Free the native object associated with this bitmap.
        srcBitmap.recycle();
        // Return the bordered circular bitmap
        return dstBitmap;
    }
    // Custom method to add a shadow around circular bitmap
    protected Bitmap addShadowToCircularBitmap(Bitmap srcBitmap, int shadowWidth, int shadowColor){
        // Calculate the circular bitmap width with shadow
        int dstBitmapWidth = srcBitmap.getWidth()+shadowWidth*2;
        Bitmap dstBitmap = Bitmap.createBitmap(dstBitmapWidth,dstBitmapWidth, Bitmap.Config.ARGB_8888);
        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(dstBitmap);
        canvas.drawBitmap(srcBitmap, shadowWidth, shadowWidth, null);
        // Paint to draw circular bitmap shadow
        Paint paint = new Paint();
        paint.setColor(shadowColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(shadowWidth);
        paint.setAntiAlias(true);
        // Draw the shadow around circular bitmap
        canvas.drawCircle (
                dstBitmapWidth / 2, // cx
                dstBitmapWidth / 2, // cy
                dstBitmapWidth / 2 - shadowWidth / 2, // Radius
                paint // Paint
        );
        srcBitmap.recycle();
        return dstBitmap;
    }

}