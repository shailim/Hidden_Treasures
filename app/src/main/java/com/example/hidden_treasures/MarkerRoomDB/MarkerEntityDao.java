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
    List<MarkerEntity> getAll();

    @Query("SELECT * FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong ORDER BY score DESC LIMIT :numMarkersToGet")
    List<MarkerEntity> loadAllWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet);

    @Insert
    void insertAll(MarkerEntity... markers);

    @Insert
    void insert(MarkerEntity marker);

    @Query("UPDATE marker_entity SET icon = :icon WHERE objectId = :id")
    void setIcon(byte[] icon, String id);

    @Query("UPDATE marker_entity SET score = :score WHERE objectId = :id")
    void updateScore(String id, int score);

    @Query("UPDATE marker_entity SET view_count = :view_count WHERE objectId = :id")
    void updateViewCount(String id, int view_count);

    @Query("UPDATE marker_entity SET last_accessed = :time WHERE objectId = :id")
    void updateLastAccessed(String id, long time);

    @Delete
    void delete(MarkerEntity marker);

    @Query("DELETE FROM marker_entity WHERE last_accessed < :time")
    void deleteOld(long time);

    @Query("DELETE FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong")
    void deleteAll(double swLat, double swLong, double neLat, double neLong);
}
