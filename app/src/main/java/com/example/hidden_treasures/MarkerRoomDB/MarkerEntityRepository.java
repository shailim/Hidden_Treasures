package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.hidden_treasures.App;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.util.GeoHash;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MarkerEntityRepository {
    private MarkerEntityDao markerEntityDao;

    MarkerEntityRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        markerEntityDao = db.markerEntityDao();
        if (ParseUser.getCurrentUser() != null) {
            updateCache();
        }
    }

    List<MarkerEntity> getWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        return markerEntityDao.loadAllWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);
    }

    LiveData<List<MarkerEntity>> getUserMarkers() {
        return markerEntityDao.getUserMarkers(ParseUser.getCurrentUser().getObjectId());
    }

    public void insert(MarkerEntity marker) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.insert(marker);
        });
    }

    public void delete(double swLat, double swLong, double neLat, double neLong, String userid) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.deleteAll(swLat, swLong, neLat, neLong, userid);
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

    public void updateLastAccessed(String id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.updateLastAccessed(id, System.currentTimeMillis());
        });
    }

    private void updateCache() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long threeDaysAgo = System.currentTimeMillis() -  (3 * 86400000);
            markerEntityDao.deleteOld( threeDaysAgo, ParseUser.getCurrentUser().getObjectId());
        });
    }
}
