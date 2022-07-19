package com.example.hidden_treasures.profile;

import com.example.hidden_treasures.models.ParseMarker;

public class ImageClickEvent {
    public final String imageKey;
    public final String title;


    public ImageClickEvent(String imageKey, String title) {
        this.imageKey = imageKey;
        this.title = title;
    }
}
