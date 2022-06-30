package com.example.hidden_treasures;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

@ParseClassName("ParseMarker")
public class ParseMarker extends ParseObject {
    public static final String MEDIA = "media";
    public static final String TITLE = "title";
    public static final String LOCATION = "location";
    public static final String VIEW_COUNT = "view_count";

    public ParseMarker() {}

    public ParseMarker(String title, ParseFile media, ParseGeoPoint location) {
        put(TITLE, title);
        if (media != null) {
            put(MEDIA, media);
        }
        put(LOCATION, location);
    }

    public ParseFile getMedia() {
        return getParseFile(MEDIA);
    }

    public String getTitle() {
        return getString(TITLE);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint(LOCATION);
    }

    public int getViewCount() {
        return (int) getNumber(VIEW_COUNT);
    }

    public void setMedia(ParseFile media) {
        put(MEDIA, media);
    }

    public void setTitle(String title) {
        put(TITLE, title);
    }

    public void setLocation(ParseGeoPoint location) {
        put(LOCATION, location);
    }

    public void setViewCount(Number viewCount) {
        put(VIEW_COUNT, viewCount);
    }
}
