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
        refreshData();
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

    public void updateViewCount(String id, int viewCount) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.updateViewCount(id, viewCount+1);
        });
    }

    public void updateScore() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (MarkerEntity marker : allMarkers.getValue()) {
                int timeDiff = (int)(System.currentTimeMillis() - marker.createdAt) / 3600000;
                int score = marker.view_count - timeDiff;
                markerEntityDao.updateScore(marker.objectId, score);
            }
        });
    }

    public void refreshData() {
        // get all com.example.hidden_treasures.markers from parse that were uploaded after the last time on app and at most 24 hours before
        AppDatabase.databaseWriteExecutor.execute(() -> {

            // Get all com.example.hidden_treasures.markers from Parse
            ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
            // TODO: find out how to not set a limit at all
            markerQuery.setLimit(10000);
            // get com.example.hidden_treasures.markers greater than when the app was last opened
            long last24hours = System.currentTimeMillis() - 24 * 3600000;
            if (AppDatabase.lastOpened > last24hours) {
                // if it's been less than a day since app was last opened
                markerQuery.whereGreaterThanOrEqualTo("time", AppDatabase.lastOpened);
            } else {
                // it's been more than a day since app was last opened, only get com.example.hidden_treasures.markers created in the last 24 hours
                markerQuery.whereGreaterThanOrEqualTo("time", last24hours);
            }
            try {
                List<ParseMarker> objects = markerQuery.find();
                for (ParseMarker object : objects) {
                    String title = object.getTitle();
                    String id = object.getRoomid();
                    long time = object.getTime();
                    double latitude = object.getLocation().getLatitude();
                    double longitude = object.getLocation().getLongitude();
                    String imageKey = object.getImage();
                    String createdBy = object.getCreatedBy();
                    int viewCount = object.getViewCount();
                    int score = object.getScore();
                    MarkerEntity marker = new MarkerEntity(id, time, title, latitude, longitude, imageKey, createdBy, viewCount, score);
                    // insert into Room database
                    markerEntityDao.insert(marker);
                }
                Log.i("Repository", "inserted " + objects.size() + " from parse");
            } catch (ParseException e) {
                Log.i("Repository", e.getMessage());
            }
        });
    }
}
