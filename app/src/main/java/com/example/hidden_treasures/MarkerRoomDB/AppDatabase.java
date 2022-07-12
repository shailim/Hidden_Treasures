package com.example.hidden_treasures.MarkerRoomDB;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database( entities = {MarkerEntity.class}, version = 1 )
public abstract class AppDatabase extends RoomDatabase {
    public abstract MarkerEntityDao markerEntityDao();
}
