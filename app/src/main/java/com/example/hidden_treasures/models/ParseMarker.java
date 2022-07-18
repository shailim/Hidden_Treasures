package com.example.hidden_treasures.models;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("ParseMarker")
public class ParseMarker extends ParseObject {
    public static final String ROOMID = "roomId";
    public static final String MEDIA = "media";
    public static final String TITLE = "title";
    public static final String LOCATION = "location";
    public static final String VIEW_COUNT = "view_count";
    public static final String SCORE = "score";
    public static final String CREATED_BY = "created_by";

    public ParseMarker() {}

    public ParseMarker(String id, String title, ParseFile media, ParseGeoPoint location) {
        put(ROOMID, id);
        put(TITLE, title);
        if (media != null) {
            put(MEDIA, media);
        }
        put(LOCATION, location);
        put(CREATED_BY, ParseUser.getCurrentUser());
    }

    public String getRoomid() { return getString(ROOMID); }

    public ParseFile getMedia() {
        return getParseFile(MEDIA);
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

    public int getViewCount() {
        //return (int) getNumber(VIEW_COUNT);
        return 0;
    }

    public int getScore() {
        //return (int) getNumber(SCORE);
        return 0;
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
