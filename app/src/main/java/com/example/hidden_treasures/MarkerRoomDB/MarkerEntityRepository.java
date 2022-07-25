package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.hidden_treasures.markers.ParseMarker;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class MarkerEntityRepository {
    private MarkerEntityDao markerEntityDao;
    private LiveData<List<MarkerEntity>> allMarkers;

    MarkerEntityRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        markerEntityDao = db.markerEntityDao();
        allMarkers = markerEntityDao.getAll();
        if (ParseUser.getCurrentUser() != null) {
            updateCache();
        }
    }

    LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    LiveData<List<MarkerEntity>> getWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        return markerEntityDao.loadAllWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);
    }

    void getFromServer(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet, List<String> ids) {
        Log.i("Repository", "ge");
        // then get markers from server
        ParseQuery<ParseMarker> query = ParseQuery.getQuery(ParseMarker.class);
        query.whereNotContainedIn("objectId", ids);
        query.setLimit(numMarkersToGet);
        ParseGeoPoint southwest = new ParseGeoPoint(swLat, swLong);
        ParseGeoPoint northeast = new ParseGeoPoint(neLat, neLong);
        query.whereWithinGeoBox("location", southwest, northeast);
        query.orderByDescending("view_count");
        query.orderByDescending("created_at");
        query.findInBackground(new FindCallback<ParseMarker>() {
            @Override
            public void done(List<ParseMarker> objects, ParseException e) {
                delete(swLat, swLong, neLat, neLong);
                storeInCache(objects);
            }
        });
    }

    void storeInCache(List<ParseMarker> objects) {
        for (ParseMarker object : objects) {
            String title = object.getTitle();
            String id = object.getObjectId();
            long time = object.getTime();
            double latitude = object.getLocation().getLatitude();
            double longitude = object.getLocation().getLongitude();
            String imageKey = object.getImage();
            String createdBy = object.getCreatedBy();
            int viewCount = object.getViewCount();
            double score = object.getScore();
            MarkerEntity marker = new MarkerEntity(id, time, title, latitude, longitude, imageKey, createdBy, viewCount, score);
            insert(marker);
        }
    }

    LiveData<List<MarkerEntity>> getUserMarkers() {
        return markerEntityDao.getUserMarkers(ParseUser.getCurrentUser().getObjectId());
    }

    public void insert(MarkerEntity marker) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.insert(marker);
        });
    }

    public void delete(double swLat, double swLong, double neLat, double neLong) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.deleteAll(swLat, swLong, neLat, neLong, ParseUser.getCurrentUser().getObjectId());
        });
    }

    public void setIcon(byte[] icon, String id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            markerEntityDao.setIcon(icon, id);
        });
    }

    public void updateViewCount(String id, int viewCount) {
        ParseQuery<ParseMarker> query = ParseQuery.getQuery(ParseMarker.class);
        query.getInBackground(id, new GetCallback<ParseMarker>() {
            public void done(ParseMarker marker, ParseException e) {
                if (e == null) {
                    marker.put("view_count", viewCount);
                    marker.saveInBackground();
                    Log.i("Repository", "updated view count");
                }
            }
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
