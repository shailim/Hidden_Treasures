package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class MarkerEntityRepository {
    private MarkerEntityDao markerEntityDao;
    private LiveData<List<MarkerEntity>> allMarkers;
    private LiveData<List<MarkerEntity>> someMarkers;

    MarkerEntityRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        markerEntityDao = db.markerEntityDao();
        allMarkers = markerEntityDao.getAll();
        List<String> list = new ArrayList<>();
        someMarkers = markerEntityDao.loadAllWithinBounds(37.4530, -122.1817, 37.4530, -122.1817, list);
    }

    LiveData<List<MarkerEntity>> getAllMarkers() {
        return allMarkers;
    }

    LiveData<List<MarkerEntity>> getWithinBounds() { return someMarkers; }

    LiveData<List<MarkerEntity>> getWithinBounds(double swLat, double swLong, double neLat, double neLong, List<String> ids) {
        return markerEntityDao.loadAllWithinBounds(swLat, swLong, neLat, neLong, ids);
    }

    void insert(MarkerEntity marker) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.insert(marker);
        });
    }
}
