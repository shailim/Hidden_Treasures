package com.example.hidden_treasures.MarkerRoomDB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MarkerEntityDao {
    @Query("SELECT * FROM marker_entity")
    LiveData<List<MarkerEntity>> getAll();

    @Query("SELECT * FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong LIMIT :numMarkersToGet")
    LiveData<List<MarkerEntity>> loadAllWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet);

    @Insert
    void insertAll(MarkerEntity... markers);

    @Insert
    void insert(MarkerEntity marker);

//    @Query("SELECT objectId, image_url, title, latitude, longitude, 'score' = view_count+created_at+23 FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong LIMIT :numMarkersToGet")
//    LiveData<List<MarkerEntity>> loadAllWithScore(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet);

    @Query("UPDATE marker_entity SET view_count = :view_count WHERE objectId = :id")
    void update(String id, int view_count);

    @Delete
    void delete(MarkerEntity marker);

    @Query("DELETE FROM marker_entity")
    void deleteAll();
}
