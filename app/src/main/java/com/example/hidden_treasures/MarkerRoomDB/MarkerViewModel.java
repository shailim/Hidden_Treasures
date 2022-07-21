package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
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

    public void delete(double swLat, double swLong, double neLat, double neLong, String userid) {
        repository.delete(swLat, swLong, neLat, neLong, userid);
    }
}
