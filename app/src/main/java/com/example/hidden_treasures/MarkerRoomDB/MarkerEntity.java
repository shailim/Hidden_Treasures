package com.example.hidden_treasures.MarkerRoomDB;

import android.location.Location;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.File;

@Entity (tableName = "marker_entity")
public class MarkerEntity {
    @PrimaryKey
    public String ObjectId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

}
