package com.example.hidden_treasures.map;

import static com.parse.Parse.getApplicationContext;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.hidden_treasures.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapSearch {

    /* Uses Google's Places API to display place name for autocomplete searching */
    public static void setupAutocompleteSearch(MapFragment mapFrag) {
        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), mapFrag.getString(R.string.maps_api_key));

        // Initialize the AutocompleteSupportFragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                mapFrag.getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                moveToSearchedLocation(mapFrag, place);
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("MapFragment", "An error occurred: " + status);
            }
        });
    }

    private static void moveToSearchedLocation(MapFragment mapFrag, @NonNull Place place) {
        // create a Geocoder object to get coordinates of place that user searched for
        Geocoder geocoder = new Geocoder(mapFrag.getContext(), new Locale("en"));
        try {
            List<Address> list = geocoder.getFromLocationName(place.getName(), 1);
            if (!list.isEmpty()) {
                // get the coordinates of the location
                Double latitude = list.get(0).getLatitude();
                Double longitude = list.get(0).getLongitude();

                // move map camera position to the location
                // right now, it's on a different thread so have to do it on the UI thread
                if (mapFrag.getActivity() != null) {
                    mapFrag.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapFrag.map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 10));
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
