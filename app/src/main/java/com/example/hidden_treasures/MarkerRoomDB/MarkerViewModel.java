package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.hidden_treasures.models.ParseMarker;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MarkerViewModel extends AndroidViewModel {

    private MarkerEntityRepository repository;

    private LiveData<List<MarkerEntity>> allMarkers;

    public MarkerViewModel(@NonNull Application application) {
        super(application);
        repository = new MarkerEntityRepository(application);
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    public LiveData<List<MarkerEntity>> getUserMarkers() {
        return repository.getUserMarkers();
    }

    // to get the next groups of com.example.hidden_treasures.markers based on location
    public List<MarkerEntity> getWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        return repository.getWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);
    }

    public void setIcon(byte[] icon, String id) {
        repository.setIcon(icon, id);
    }

    public void updateViewCount(String id, int viewCount) { repository.updateViewCount(id, viewCount); }

    public void updateLastAccessed(String id) {
        repository.updateLastAccessed(id);
    }

    public void insertMarker(MarkerEntity marker) {
        repository.insert(marker);
    }

    public void storeNewMarkers(List<ParseMarker> objects) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
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
                repository.insert(marker);
            }
        });
    }

    public void delete(double swLat, double swLong, double neLat, double neLong) {
        repository.delete(swLat, swLong, neLat, neLong);
    }
}
