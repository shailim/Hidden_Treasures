package com.example.hidden_treasures.MarkerRoomDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

@Dao
public interface MarkerEntityDao {
    @Query("SELECT * FROM marker_entity")
    List<MarkerEntity> getAll();

    @Query("SELECT * FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong ")
    List<MarkerEntity> loadAllWithinBounds(double swLat, double swLong, double neLat, double neLong);

    @Insert
    void insertAll(MarkerEntity... markers);

    @Delete
    void delete(MarkerEntity marker);
}
