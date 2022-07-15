package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.hidden_treasures.App;
import com.example.hidden_treasures.models.ParseMarker;
import com.parse.ParseException;
import com.parse.ParseQuery;

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
        someMarkers = markerEntityDao.loadAllWithinBounds(37.4530, -122.1817, 37.4530, -122.1817, 50);
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

    public void refreshData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            //markerEntityDao.deleteAll();
            List<String> markerIds = new ArrayList<>();
            for (MarkerEntity marker : allMarkers.getValue()) {
                markerIds.add(marker.objectId);
            }

            // Get all markers from Parse
            ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
            // TODO: find out how to not set a limit at all
            markerQuery.setLimit(10000);
            markerQuery.whereNotContainedIn("ObjectId", markerIds);
            try {
                List<ParseMarker> objects = markerQuery.find();
                for (ParseMarker object : objects) {
                    String title = object.getTitle();
                    String id = object.getObjectId();
                    long createdAt = object.getCreatedAt().getTime();
                    double latitude = object.getLocation().getLatitude();
                    double longitude = object.getLocation().getLongitude();
                    String imageUrl = object.getMedia().getUrl();
                    String createdBy = object.getCreatedBy();
                    int viewCount = object.getViewCount();
                    MarkerEntity marker = new MarkerEntity(id, createdAt, title, latitude, longitude, imageUrl, createdBy, viewCount);
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
