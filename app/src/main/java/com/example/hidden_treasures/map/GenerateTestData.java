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
import java.util.UUID;

/* Creates marker objects using sample place data */
public class GenerateTestData {

    private static final String TAG = "GenerateTestData";

    public List<ParseMarker> sampleData = new ArrayList<>();

    /* Gets place data from sample file and creates new com.example.hidden_treasures.markers to populate map */
    public void getData(Context c) throws IOException {

        // get list of places in sample data
        JSONArray placesList = getPlacesList(c);

        // create the new com.example.hidden_treasures.markers
        // use this key to access the same image in s3
        String key = "3c6216c8d3";
        createMarkers(key, placesList);

        // save list of com.example.hidden_treasures.markers to parse
        ParseMarker.saveAllInBackground(sampleData, new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Log.i(TAG, "saved all com.example.hidden_treasures.markers");
                } else {
                    Log.i(TAG, "couldn't save com.example.hidden_treasures.markers");
                }
            }
        });
    }

    /* Creates new com.example.hidden_treasures.markers with the sample data */
    private void createMarkers(String imageKey, JSONArray placesList) {
        try {
            int numMarkersToMake = 20;
            for (int i = 0; i < placesList.length(); i++) {
                JSONObject object = placesList.getJSONObject(i);

                // generate an id
                String id = UUID.randomUUID().toString();

                // get the latitude and longitude coordinates
                double latitude = object.getDouble("lat");
                double longitude = object.getDouble("lng");

                // create the geoPoint object
                ParseGeoPoint geoPoint = new ParseGeoPoint(latitude, longitude);

                // get the place name for title
                String title = object.getString("toponymName");

                // random view_count and score
                int view_count = (int)(Math.random() * 1001) + 1;
                int score = (int)(Math.random() * 101) + 1;

                // get current time
                long time = System.currentTimeMillis();

                // create a new parse marker object with all that
                ParseMarker newMarker = new ParseMarker(id, title, imageKey, geoPoint, time);
                newMarker.setScore(score);
                newMarker.setViewCount(view_count);

                // add it to the list
                sampleData.add(newMarker);
                if (numMarkersToMake > 0) {
                    for (int j = 1; j < numMarkersToMake; j++) {
                        String id2 = UUID.randomUUID().toString();
                        int randomNum = (int) (Math.random() * -10 + 5);
                        ParseGeoPoint geoPoint2 = new ParseGeoPoint(latitude + randomNum, longitude + randomNum);
                        newMarker = new ParseMarker(id2, title, imageKey, geoPoint2, time);
                        newMarker.setScore(score);
                        newMarker.setViewCount(view_count);
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
}
