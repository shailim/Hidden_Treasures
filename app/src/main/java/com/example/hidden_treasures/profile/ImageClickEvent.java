package com.example.hidden_treasures.profile;

import java.util.Date;

public class ImageClickEvent {
    public final String imageKey;
    public final String title;
    public final int viewCount;
    public Date date;


    public ImageClickEvent(String imageKey, String title, int viewCount, Date date) {
        this.imageKey = imageKey;
        this.title = title;
        this.viewCount = viewCount;
        this.date = date;
    }
}
