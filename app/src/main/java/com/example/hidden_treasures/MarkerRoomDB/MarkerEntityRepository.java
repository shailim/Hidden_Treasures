package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.hidden_treasures.App;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.util.GeoHash;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MarkerEntityRepository {
    private MarkerEntityDao markerEntityDao;
    private LiveData<List<MarkerEntity>> allMarkers;
    private LiveData<List<MarkerEntity>> someMarkers;

    MarkerEntityRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        markerEntityDao = db.markerEntityDao();
        allMarkers = markerEntityDao.getAll();
        String geohash = GeoHash.encode(37.4530, -122.1817, 6);
        LatLngBounds bound = GeoHash.bounds(geohash);
        someMarkers = markerEntityDao.loadAllWithinBounds(bound.southwest.latitude, bound.southwest.longitude, bound.northeast.latitude, bound.northeast.longitude, 50);
    }

    LiveData<List<MarkerEntity>> getAllMarkers() {
        return allMarkers;
    }

    LiveData<List<MarkerEntity>> getWithinBounds() { return someMarkers; }

    LiveData<List<MarkerEntity>> getWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        return markerEntityDao.loadAllWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);
    }


    public void insert(MarkerEntity marker) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.insert(marker);
        });
    }

    public void delete() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.deleteAll();
        });
    }

    public void setIcon(byte[] icon, String id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.setIcon(icon, id);
        });
    }

    public void updateViewCount(String id, int viewCount) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.updateViewCount(id, viewCount+1);
        });
    }

    // update score of all markers to reflect new view counts and time differences
    public void updateScore() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (MarkerEntity marker : allMarkers.getValue()) {
                Log.i("Repository", "updating score");
                int timeDiff = (int)(System.currentTimeMillis() - marker.createdAt) / 3600000;
                int score = marker.view_count - timeDiff;
                markerEntityDao.updateScore(marker.objectId, score);
            }
        });
    }
}
