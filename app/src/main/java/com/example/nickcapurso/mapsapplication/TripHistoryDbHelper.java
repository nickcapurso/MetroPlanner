package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static com.example.nickcapurso.mapsapplication.TripHistoryDbInfo.*;
import static com.example.nickcapurso.mapsapplication.TripHistoryDbInfo.TripHistoryEntry.*;
/**
 * Created by nickcapurso on 4/14/15.
 */
public class TripHistoryDbHelper extends SQLiteOpenHelper {

    public TripHistoryDbHelper(Context context){
        super(context, TABLE_NAME, null,TABLE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_NEW_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
