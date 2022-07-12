package com.example.hidden_treasures.MarkerRoomDB;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.File;
import java.sql.Date;

@Entity (tableName = "marker_entity")
public class MarkerEntity {
    @PrimaryKey
    @NonNull
    public String objectId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    @ColumnInfo(name = "created_by")
    public String createdBy;

    public MarkerEntity(String objectId, long createdAt, String title, double latitude, double longitude, String imageUrl, String createdBy) {
        this.objectId = objectId;
        this.createdAt = createdAt;
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.createdBy = createdBy;
    }

}
