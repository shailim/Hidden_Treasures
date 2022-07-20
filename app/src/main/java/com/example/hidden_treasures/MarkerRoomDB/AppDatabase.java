package com.example.hidden_treasures.MarkerRoomDB;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.hidden_treasures.models.ParseMarker;
import com.google.android.gms.maps.model.Marker;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database( entities = {MarkerEntity.class}, version = 1 )
public abstract class AppDatabase extends RoomDatabase {

    public abstract MarkerEntityDao markerEntityDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    // to run room database operations on a different thread than the ui thread
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static volatile long lastOpened;

    // to get the room database instance
    public static AppDatabase getDatabase(final Context context) {
        // create a new room database instance if it doesn't already exist
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .build();
                    lastOpened = System.currentTimeMillis();
                }
            }
        }
        return INSTANCE;
    }
}
