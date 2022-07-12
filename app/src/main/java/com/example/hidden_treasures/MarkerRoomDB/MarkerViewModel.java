package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MarkerViewModel extends AndroidViewModel {

    private MarkerEntityRepository repository;

    private final LiveData<List<MarkerEntity>> allMarkers;
    private final List<MarkerEntity> someMarkers;

    public MarkerViewModel(@NonNull Application application) {
        super(application);
        repository = new MarkerEntityRepository(application);
        allMarkers = repository.getAllMarkers();
        // the default area to get the initial set of markers from if a previous state was not saved
        someMarkers = repository.getWithinBounds(37.4530, -122.1817, 37.4530, -122.1817);
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    public List<MarkerEntity> getWithinBounds() { return someMarkers; }

    public void setSomeMarkers(List<MarkerEntity> markers) {
        someMarkers.clear();
        someMarkers.addAll(markers);
    }

    // to get the next groups of markers based on location
    public List<MarkerEntity> getWithinBounds(double swLat, double swLong, double neLat, double neLong) {
        return repository.getWithinBounds(swLat, swLong, neLat, neLong);
    }

    public void insertMarker(MarkerEntity marker) {
        repository.insert(marker);
    }
}
