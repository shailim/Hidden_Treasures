package com.example.hidden_treasures.profile;

import com.example.hidden_treasures.models.ParseMarker;

public class ImageClickEvent {
    public final String imageUrl;
    public final String title;


    public ImageClickEvent(String imageUrl, String title) {
        this.imageUrl = imageUrl;
        this.title = title;
    }
}
