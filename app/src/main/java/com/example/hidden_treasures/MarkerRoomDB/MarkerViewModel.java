package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MarkerViewModel extends AndroidViewModel {

    private MarkerEntityRepository repository;

    private final LiveData<List<MarkerEntity>> allMarkers;

    public MarkerViewModel(@NonNull Application application) {
        super(application);
        repository = new MarkerEntityRepository(application);
        allMarkers = repository.getAllMarkers();
    }

   public LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    public void insertMarker(MarkerEntity marker) {
        repository.insert(marker);
    }
}
