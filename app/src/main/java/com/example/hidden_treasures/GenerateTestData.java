package com.example.hidden_treasures;

import android.content.Context;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GenerateTestData {

    private static final String TAG = "GenerateTestData";

    public List<ParseMarker> sampleData = new ArrayList<>();

    public GenerateTestData() {}

    /* Parses json file with locations data */
    public void getData(Context c) throws IOException {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();

        try (InputStreamReader reader = new InputStreamReader(c.getAssets().open("searchJSON.json"))) {

            // get the image used before
            ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
            ParseMarker marker = markerQuery.get("1J0Qw5BLDV");
            ParseFile image = marker.getMedia();

            String obj = jsonParser.parse(reader).toString();

            //JSONObject jo = (JSONObject) obj;
            JSONObject jo = new JSONObject(obj);

            JSONArray placesList = jo.getJSONArray("geonames");
            Log.i(TAG, String.valueOf(placesList.length()));
            int numMarkersToMake = 10;
            for (int i = 0; i < placesList.length(); i++) {
                JSONObject object = placesList.getJSONObject(i);

                // get the latitude and longitude coordinates
                Double latitude = object.getDouble("lat");
                Double longitude = object.getDouble("lng");

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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }
    }

}
