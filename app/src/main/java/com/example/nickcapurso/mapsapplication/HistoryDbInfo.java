package com.example.nickcapurso.mapsapplication;

import android.provider.BaseColumns;

/**
 * Created by nickcapurso on 4/14/15.
 */
public final class HistoryDbInfo {
    public static abstract class HistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "TripHistory";
        public static final int TABLE_VERSION = 1;

        public static final String START_STATION = "startstation";
        public static final String START_LAT = "startlat";
        public static final String START_LON = "startlon";
        public static final String END_STATION = "endstation";
        public static final String END_LAT = "endlat";
        public static final String END_LON = "endlon";
        public static final String DATE = "date";
    }

    public static final String DATABASE_NAME = "TripHistory.db";
    public static final String SQL_CREATE_NEW_TABLE =
            "CREATE TABLE " + HistoryEntry.TABLE_NAME + " (" +
                    HistoryEntry._ID + " INTEGER PRIMARY KEY," +
                    HistoryEntry.START_STATION + " TEXT," +
                    HistoryEntry.START_LAT + " REAL," +
                    HistoryEntry.START_LON + " REAL," +
                    HistoryEntry.END_STATION + " TEXT," +
                    HistoryEntry.END_LAT + " REAL," +
                    HistoryEntry.END_LON + " REAL," +
                    HistoryEntry.DATE + " TEXT)";

    public static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + HistoryEntry.TABLE_NAME;
}
