package com.example.nickcapurso.mapsapplication;

import android.provider.BaseColumns;

/**
 * Created by nickcapurso on 4/14/15.
 */
public final class TripHistoryDbInfo {
    public static abstract class TripHistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "TripHistory";
        public static final int TABLE_VERSION = 1;

        public static final String START_STATION = "startstation";
        public static final String TRANSFER_STATION = "transferstation";
        public static final String END_STATION = "endstation";
    }

    public static final String DATABASE_NAME = "TripHistory.db";
    public static final String SQL_CREATE_NEW_TABLE =
            "CREATE TABLE " + TripHistoryEntry.TABLE_NAME + " (" +
                    TripHistoryEntry._ID + " INTEGER PRIMARY KEY," +
                    TripHistoryEntry.START_STATION + " TEXT," +
                    TripHistoryEntry.TRANSFER_STATION + " TEXT," +
                    TripHistoryEntry.END_STATION + " TEXT )";

    public static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS " + TripHistoryEntry.TABLE_NAME;
}
