package com.example.hidden_treasures.models;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("ParseMarker")
public class ParseMarker extends ParseObject {
    public static final String ROOMID = "roomId";
    public static final String IMAGE_KEY = "image_key";
    public static final String TITLE = "title";
    public static final String LOCATION = "location";
    public static final String VIEW_COUNT = "view_count";
    public static final String SCORE = "score";
    public static final String CREATED_BY = "created_by";
    public static final String TIME = "time";

    public ParseMarker() {}

    public ParseMarker(String id, String title, String imageKey, ParseGeoPoint location, long time) {
        put(ROOMID, id);
        put(TITLE, title);
        put(IMAGE_KEY, imageKey);
        put(LOCATION, location);
        put(CREATED_BY, ParseUser.getCurrentUser());
        put(TIME, time);
    }

    public String getRoomid() { return getString(ROOMID); }

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
        return getString(CREATED_BY);
    }

    public long getTime() { return (long) getNumber(TIME); }

    public int getViewCount() {
        return (int) getNumber(VIEW_COUNT);
    }

    public int getScore() {
        return (int) getNumber(SCORE);
    }

    public void setTitle(String title) {
        put(TITLE, title);
    }

    public void setViewCount(int num) {
        put(VIEW_COUNT, num);
    }

    public void setScore(int num) {
        put(SCORE, num);
    }
}
