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
    private final LiveData<List<MarkerEntity>> someMarkers;

    public MarkerViewModel(@NonNull Application application) {
        super(application);
        repository = new MarkerEntityRepository(application);
        allMarkers = repository.getAllMarkers();

        // the default area to get the initial set of markers from if a previous state was not saved
        someMarkers = repository.getWithinBounds();
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    public LiveData<List<MarkerEntity>> getWithinBounds() { return someMarkers; }

    // to get the next groups of com.example.hidden_treasures.markers based on location
    public LiveData<List<MarkerEntity>> getWithinBounds(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        return repository.getWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);
    }

    public void setIcon(byte[] icon, String id) {
        repository.setIcon(icon, id);
    }

    public void updateViewCount(String id, int viewCount) { repository.updateViewCount(id, viewCount); }
    public void updateScore() {
        repository.updateScore();
    }

    public void insertMarker(MarkerEntity marker) {
        repository.insert(marker);
    }

    public void delete() {
        repository.delete();
    }
}
