package com.example.hidden_treasures.map;

import android.util.Log;

import com.example.hidden_treasures.util.GeoHash;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapHelper {

    public static final String TAG = "MapFragment";

    // according to the geohash of the marker, stores it into the marker table
    public static HashMap<String, HashMap<String, List<Marker>>> addToMarkerTable(HashMap<String, HashMap<String, List<Marker>>> markerTable, Marker marker) {
        // get geohash of marker
        // TODO: when uploading markers, generate their geohash, for testing I'm just generating here
        String hash = GeoHash.encode(marker.getPosition().latitude, marker.getPosition().longitude, 7);
        // if an entry for the geohash prefix of 4 characters doesn't exist
        if (markerTable.get(hash.substring(0, 2)) == null) {
            addIntoFirstTable(markerTable, hash, marker);
            // if an entry in the inner table for geohashes of precision length 7 doesn't exist
        } else if (markerTable.get(hash.substring(0, 2)).get(hash.substring(0, 4)) == null) {
            addIntoSecondTable(markerTable, hash, marker);
        } else {
            // add the marker to the list
            addIntoGeohashList(markerTable, hash, marker);
        }
        return markerTable;
    }

    // creates a new hash map entry into markerTable
    public static void addIntoFirstTable(HashMap<String, HashMap<String, List<Marker>>> markerTable, String hash, Marker marker) {
        HashMap<String, List<Marker>> newTable = new HashMap<>();
        List<Marker> list = new ArrayList<>();
        list.add(marker);
        // create a new hash table with a list value
        newTable.put(hash.substring(0, 4), list);
        markerTable.put(hash.substring(0, 2), newTable);
        Log.i(TAG, "added new entry to first table");
    }

    // creates a new marker list entry into an inner hash map
    public static void addIntoSecondTable(HashMap<String, HashMap<String, List<Marker>>> markerTable, String hash, Marker marker) {
        List<Marker> list = new ArrayList<>();
        list.add(marker);
        // create a new entry with a list value
        markerTable.get(hash.substring(0, 2)).put(hash.substring(0, 4), list);
        Log.i(TAG, "added new entry to second table");
    }

    // inserts marker into the appropriate list
    public static void addIntoGeohashList(HashMap<String, HashMap<String, List<Marker>>> markerTable, String hash, Marker marker) {
        // add the marker to the list
        markerTable.get(hash.substring(0, 2)).get(hash.substring(0, 4)).add(marker);
        Log.i(TAG, "added to list in marker table: " + hash);
    }
}
