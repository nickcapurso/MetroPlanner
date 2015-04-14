package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.LinearLayout;

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

        mMainLayout = (LinearLayout)findViewById(R.id.incidentLayout);
    }


    class DatabaseAccess extends AsyncTask<String,Void,String> {
        HistoryDbHelper dbHelper;
        SQLiteDatabase db;

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
            mDialog.cancel();

        }
    }
}
