package com.example.hidden_treasures.collections;

import com.example.hidden_treasures.markers.ParseMarker;
import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("Collections")
public class ParseCollection extends ParseObject {

    public static final String MARKER = "marker";
    public static final String USERID = "userId";

    public ParseCollection() {}

    public ParseCollection(ParseMarker marker, String userId) {
        put(MARKER, marker);
        put(USERID, userId);
    }

    public ParseObject getMarker() {
        return getParseObject(MARKER);
    }

    public String getUserId() {
        return getString(USERID);
    }


}
