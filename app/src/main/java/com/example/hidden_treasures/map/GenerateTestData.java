package com.example.hidden_treasures.map;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.hidden_treasures.models.ParseMarker;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/* Creates marker objects using sample place data */
public class GenerateTestData {

    private static final String TAG = "GenerateTestData";

    public List<ParseMarker> sampleData = new ArrayList<>();

    /* Gets place data from sample file and creates new markers to populate map */
    public void getData(Context c) throws IOException {

        // reuse same image for all the new sample markers
        ParseFile image = getParseImage();

        // get list of places in sample data
        JSONArray placesList = getPlacesList(c);

        // create the new markers
        createMarkers(image, placesList);

        // save list of markers to parse
        ParseMarker.saveAllInBackground(sampleData, new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Log.i(TAG, "saved all markers");
                } else {
                    Log.i(TAG, "couldn't save markers");
                }
            }
        });
    }

    /* Creates new markers with the sample data */
    private void createMarkers(ParseFile image, JSONArray placesList) {
        try {
            int numMarkersToMake = 20;
            for (int i = 0; i < placesList.length(); i++) {
                JSONObject object = placesList.getJSONObject(i);

                // get the latitude and longitude coordinates
                double latitude = object.getDouble("lat");
                double longitude = object.getDouble("lng");

                // create the geoPoint object
                ParseGeoPoint geoPoint = new ParseGeoPoint(latitude, longitude);

                // get the place name for title
                String title = object.getString("toponymName");

                // create a new parse marker object with all that
                ParseMarker newMarker = new ParseMarker(title, image, geoPoint);

                // add it to the list
                sampleData.add(newMarker);
                if (numMarkersToMake > 0) {
                    for (int j = 1; j < numMarkersToMake; j++) {
                        int randomNum = (int) (Math.random() * -10 + 5);
                        ParseGeoPoint geoPoint2 = new ParseGeoPoint(latitude + randomNum, longitude + randomNum);
                        newMarker = new ParseMarker(title, image, geoPoint2);
                        sampleData.add(newMarker);
                    }
                    numMarkersToMake--;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private JSONArray getPlacesList(Context c) {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
        JSONArray placesList = null;

        try (InputStreamReader reader = new InputStreamReader(c.getAssets().open("searchJSON.json"))) {
            String obj = jsonParser.parse(reader).toString();
            JSONObject jo = new JSONObject(obj);

            placesList = jo.getJSONArray("geonames");
            Log.i(TAG, String.valueOf(placesList.length()));
        } catch (org.json.simple.parser.ParseException e) {
            Log.i(TAG, e.getMessage());
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        } catch(IOException e) {
            Log.i(TAG, e.getMessage());
        }
        return placesList;
    }

    @Nullable
    private ParseFile getParseImage() {
        // get the image used before
        ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
        ParseFile image = null;
        ParseMarker marker = null;
        try {
            marker = markerQuery.get("gfwdSfxouE");
            image = marker.getMedia();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return image;
    }
}
