package com.example.hidden_treasures.markers;

import java.util.Date;

public class MarkerData {
    private String id;
    private int viewCount;
    private Date date;
    private String imageKey;

    public MarkerData(String id, int viewCount, Date date, String imageKey) {
        this.id = id;
        this.viewCount = viewCount;
        this.date = date;
        this.imageKey = imageKey;
    }

    public String getId() {
        return id;
    }

    public int getViewCount() {
        return viewCount;
    }

    public Date getDate() {
        return date;
    }

    public String getImageKey() {
        return imageKey;
    }
}
