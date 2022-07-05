package com.example.hidden_treasures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MarkerItem implements ClusterItem {

    private LatLng position;
    private String title;
    private String tag;

    public MarkerItem(double lat, double lng, String title, String tag) {
        position = new LatLng(lat, lng);
        this.title = title;
        this.tag = tag;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }

    @Nullable
    public String getTag() {
        return tag;
    }
}
