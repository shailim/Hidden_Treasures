package com.example.hidden_treasures.MarkerRoomDB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MarkerEntityDao {
    @Query("SELECT * FROM marker_entity")
    LiveData<List<MarkerEntity>> getAll();

    @Query("SELECT * FROM marker_entity WHERE latitude > :swLat AND latitude < :neLat AND longitude > :swLong AND longitude < :neLong AND objectId NOT IN (:ids)")
    LiveData<List<MarkerEntity>> loadAllWithinBounds(double swLat, double swLong, double neLat, double neLong, List<String> ids);

    @Query("SELECT * FROM marker_entity WHERE latitude > 37")
    List<MarkerEntity> getSome();

    @Query("CREATE TEMP TABLE lng_list(marker_id)")
    void createMarkerIdTable();

    @Query("INSERT INTO lng_list(marker_id) VALUES (:id)")
    void insertMarkerId(String id);

    @Insert
    void insertAll(MarkerEntity... markers);

    @Insert
    void insert(MarkerEntity marker);

    @Delete
    void delete(MarkerEntity marker);

    @Query("DELETE FROM marker_entity")
    void deleteAll();
}
