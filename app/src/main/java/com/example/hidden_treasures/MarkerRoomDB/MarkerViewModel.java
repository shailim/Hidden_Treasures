package com.example.hidden_treasures.MarkerRoomDB;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.airbnb.lottie.L;
import com.example.hidden_treasures.models.ParseMarker;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class MarkerViewModel extends AndroidViewModel {

    private MarkerEntityRepository repository;

    private LiveData<List<MarkerEntity>> allMarkers;

    public MarkerViewModel(@NonNull Application application) {
        super(application);
        repository = new MarkerEntityRepository(application);
        allMarkers = repository.getAllMarkers();
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() { return allMarkers; }

    public LiveData<List<MarkerEntity>> getUserMarkers() {
        return repository.getUserMarkers();
    }


    public LiveData<List<MarkerEntity>> getFromCache(double swLat, double swLong, double neLat, double neLong, int numMarkersToGet) {
        Callable<LiveData<List<MarkerEntity>>> callable = () -> repository.getWithinBounds(swLat, swLong, neLat, neLong, numMarkersToGet);

        Future<LiveData<List<MarkerEntity>>> future = AppDatabase.databaseWriteExecutor.submit(callable);
        try {
            allMarkers = future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<String> ids = new ArrayList<>();
        if (allMarkers.getValue() != null) {
            for (MarkerEntity marker : allMarkers.getValue()) {
                ids.add(marker.objectId);
                updateLastAccessed(marker.objectId);
            }
        }
        repository.getFromServer(swLat, swLong, neLat, neLong, numMarkersToGet, ids);
        return allMarkers;
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

    public void delete(double swLat, double swLong, double neLat, double neLong) {
        repository.delete(swLat, swLong, neLat, neLong);
    }
}
