package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.LinearLayout;

import static com.example.nickcapurso.mapsapplication.HistoryDbInfo.HistoryEntry;
/**
 * Created by cheng on 4/14/15.
 */
public class HistoryActivity extends Activity {
    private LinearLayout mMainLayout;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mMainLayout = (LinearLayout)findViewById(R.id.historyLayout);

    }

    @Override
    public void onResume(){
        super.onResume();
        new DatabaseAccess().execute();
    }


    class DatabaseAccess extends AsyncTask<String,Void,String> {
        private final String[] projection = {HistoryEntry._ID, HistoryEntry.START_STATION,HistoryEntry.END_STATION,HistoryEntry.DATE};
        private final String sortOrder = HistoryEntry._ID + " DESC";

        private HistoryDbHelper dbHelper;
        private SQLiteDatabase db;

        public DatabaseAccess(){
            dbHelper = new HistoryDbHelper(HistoryActivity.this);
        }


        @Override
        protected void onPreExecute(){
            mDialog = ProgressDialog.show(HistoryActivity.this, "Please Wait...", "Reading trip history...", true);
        }

        @Override
        protected String doInBackground(String... params) {
            db = dbHelper.getReadableDatabase();


            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Cursor cursor;
            String startingStation, endingStation, date;

            cursor = db.query(HistoryEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);
            cursor.moveToFirst();

            startingStation= "Start: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_STATION));
            endingStation= "End: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_STATION));
            date= "Date: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.DATE));
            mMainLayout.addView(new HistoryView(HistoryActivity.this, startingStation, endingStation, date));
            mMainLayout.addView(new ShadowView(HistoryActivity.this));

            while(cursor.moveToNext()){
                startingStation= "Start: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_STATION));
                endingStation= "End: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_STATION));
                date= "Date: " + cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.DATE));
                mMainLayout.addView(new HistoryView(HistoryActivity.this, startingStation, endingStation, date));
                mMainLayout.addView(new ShadowView(HistoryActivity.this));
            }

            mDialog.cancel();
        }
    }
}
