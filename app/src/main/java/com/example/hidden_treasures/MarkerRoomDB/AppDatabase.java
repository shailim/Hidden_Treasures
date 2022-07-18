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

    // to get the room database instance
    public static AppDatabase getDatabase(final Context context) {
        // create a new room database instance if it doesn't already exist
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .addCallback(roomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /* Callback to prepopulate data into the database upon first creating it */
    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                // Update data with markers from parse
                MarkerEntityDao dao = INSTANCE.markerEntityDao();
                dao.deleteAll();

                // Get all markers from Parse to populate the database
                ParseQuery<ParseMarker> markerQuery = ParseQuery.getQuery(ParseMarker.class);
                // TODO: find out how to not set a limit at all
                markerQuery.setLimit(10000);
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
                        int score = object.getScore();
                        MarkerEntity marker = new MarkerEntity(id, createdAt, title, latitude, longitude, imageUrl, createdBy, viewCount, score);
                        dao.insert(marker);
                    }
                    Log.i("AppDatabase", "inserted all markers");
                } catch (ParseException e) {
                    Log.i("AppDatabase", e.getMessage());
                }
            });
        }
    };
}
