package com.spot.civicpin.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CivicReportEntity.class, ReportEventEntity.class}, version = 1, exportSchema = false)
public abstract class CivicDatabase extends RoomDatabase {

    private static volatile CivicDatabase INSTANCE;

    public abstract CivicReportDao reportDao();
    public abstract ReportEventDao eventDao();

    public static CivicDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CivicDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CivicDatabase.class,
                            "civic_pin_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
