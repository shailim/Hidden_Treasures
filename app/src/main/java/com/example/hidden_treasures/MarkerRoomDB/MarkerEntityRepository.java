package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class MarkerEntityRepository {
    private MarkerEntityDao markerEntityDao;
    private LiveData<List<MarkerEntity>> allMarkers;

    MarkerEntityRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        markerEntityDao = db.markerEntityDao();
        allMarkers = markerEntityDao.getAll();
    }

    LiveData<List<MarkerEntity>> getAllMarkers() {
        return allMarkers;
    }

    void insert(MarkerEntity marker) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.insert(marker);
        });
    }
}
