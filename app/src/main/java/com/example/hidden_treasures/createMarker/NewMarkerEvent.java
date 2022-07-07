package com.example.hidden_treasures.createMarker;

import com.example.hidden_treasures.models.ParseMarker;

public class NewMarkerEvent {
    public final ParseMarker marker;

    public NewMarkerEvent(ParseMarker marker) {
        this.marker = marker;
    }
}
