package com.example.hidden_treasures;

import android.content.Context;
import android.util.Log;

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

        try (InputStreamReader reader = new InputStreamReader(c.getAssets().open("places.json"))) {
            Log.i(TAG, "found places.json");
            String obj = jsonParser.parse(reader).toString();

            //JSONObject jo = (JSONObject) obj;
            JSONObject jo = new JSONObject(obj);

            JSONArray placesList = jo.getJSONArray("geonames");
            for (int i = 0; i < 3; i++) {
                JSONObject object = placesList.getJSONObject(i);
                Log.i(TAG, String.valueOf(object.getDouble("lng")));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
