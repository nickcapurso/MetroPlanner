package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static com.example.nickcapurso.mapsapplication.HistoryDbInfo.*;
import static com.example.nickcapurso.mapsapplication.HistoryDbInfo.HistoryEntry.*;
/**
 * Created by nickcapurso on 4/14/15.
 */
public class HistoryDbHelper extends SQLiteOpenHelper {

    public HistoryDbHelper(Context context){
        super(context, TABLE_NAME, null,TABLE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_NEW_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void recreateTable(SQLiteDatabase db){
        db.execSQL(SQL_DELETE_TABLE);
        db.execSQL(SQL_CREATE_NEW_TABLE);
    }
}
