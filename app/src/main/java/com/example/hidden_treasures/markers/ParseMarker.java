package com.example.hidden_treasures.markers;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("ParseMarker")
public class ParseMarker extends ParseObject {
    public static final String IMAGE_KEY = "image_key";
    public static final String TITLE = "title";
    public static final String LOCATION = "location";
    public static final String VIEW_COUNT = "view_count";
    public static final String SCORE = "score";
    public static final String CREATED_BY = "created_by";
    public static final String TIME = "time";

    public ParseMarker() {}

    public ParseMarker(String title, String imageKey, ParseGeoPoint location, long time, double score) {
        put(TITLE, title);
        put(IMAGE_KEY, imageKey);
        put(LOCATION, location);
        put(CREATED_BY, ParseUser.getCurrentUser());
        put(TIME, time);
        put(SCORE, score);
    }

    public String getImage() {
        return getString(IMAGE_KEY);
    }

    public String getTitle() {
        return getString(TITLE);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint(LOCATION);
    }

    public String getCreatedBy() {
        return getParseUser(CREATED_BY).getObjectId();
    }

    public long getTime() { return (long) getNumber(TIME); }

    public int getViewCount() {
        return (int) getNumber(VIEW_COUNT);
    }

    public double getScore() {
        return (double)getNumber(SCORE);
    }

    public byte[] getIcon() { return null; }

    public void setTitle(String title) {
        put(TITLE, title);
    }

    public void setViewCount(int num) {
        put(VIEW_COUNT, num);
    }

    public void setScore(double num) {
        put(SCORE, num);
    }
}
