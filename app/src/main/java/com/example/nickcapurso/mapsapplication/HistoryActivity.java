package com.example.nickcapurso.mapsapplication;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import static com.example.nickcapurso.mapsapplication.HistoryDbInfo.HistoryEntry;
/**
 * Created by cheng on 4/14/15.
 */
public class HistoryActivity extends ActionBarActivity implements LocationListener{
    private static final String RESAVE_KEY = "resave";
    private static final String GET_HISTORY = "getHistory";
    private static final String CLEAR_HISTORY = "clearHistory";
    private static final String UPDATE_HISTORY = "updateHistory";
    public static final String CURRENT_LOCATION = "Current Location";

    private boolean mResave;

    private ArrayList<SixTuple> mHistoryCoords = new ArrayList<SixTuple>();
    private SixTuple mCurrentSelection;
    private LinearLayout mMainLayout;
    private ProgressDialog mDialog;
    private LocationManager mLocationManager;
    private NetworkTimeout mTimer;

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        mMainLayout = (LinearLayout)findViewById(R.id.historyLayout);

        mPrefs = getPreferences(Context.MODE_PRIVATE);
        mPrefsEditor = mPrefs.edit();
        mResave = mPrefs.getBoolean(RESAVE_KEY, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_history, menu);

        menu.findItem(R.id.history_resave).setChecked(mResave);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.history_clear:
                new DatabaseAccess().execute(CLEAR_HISTORY);
                return true;
            case R.id.history_resave:
                item.setChecked(item.isChecked()? false : true);
                mResave = item.isChecked();
                savePreferences();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        mMainLayout.removeAllViews();
        new DatabaseAccess().execute(GET_HISTORY);
    }

    private void startMapsActivity(){
        Intent startMaps = new Intent(HistoryActivity.this, MapsActivity.class);
        startMaps.putExtra("startingAddr", new AddressInfo(mCurrentSelection.starting, mCurrentSelection.startLat, mCurrentSelection.startLon));
        startMaps.putExtra("endingAddr", new AddressInfo(mCurrentSelection.ending, mCurrentSelection.endLat, mCurrentSelection.endLon));
        startActivity(startMaps);
    }

    private void savePreferences(){
        mPrefsEditor.putBoolean(RESAVE_KEY, mResave);
        mPrefsEditor.commit();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case HandlerCodes.HISTORY_ENTRY_CHOSEN:
                    int index = mHistoryCoords.size() - ((int)message.obj);
                    mCurrentSelection = mHistoryCoords.get(index);

                    if(mCurrentSelection.starting.equals(CURRENT_LOCATION)){
                        mDialog = ProgressDialog.show(HistoryActivity.this, "Please Wait...", "Obtaining Location Fix", true);
                        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, HistoryActivity.this, null);
                        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, HistoryActivity.this, null);
                        mTimer = new NetworkTimeout(mHandler);
                        mTimer.start();
                    }else{
                        if(mResave) {
                            new DatabaseAccess().execute(UPDATE_HISTORY);
                        }else{
                            startMapsActivity();
                        }
                    }
                    Log.d(MainActivity.TAG, "Chosen entry: " + index);
                    break;
                case HandlerCodes.TIMEOUT:
                    Toast.makeText(HistoryActivity.this, "Unable to get location fix.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        mTimer.cancel();
        mDialog.cancel();
        mLocationManager.removeUpdates(HistoryActivity.this);
        Log.d(MainActivity.TAG, "Obtained location - lat: " + location.getLatitude() + ", lng: " + location.getLongitude());
        mCurrentSelection.startLat = location.getLatitude();
        mCurrentSelection.startLon = location.getLongitude();
        if(mResave) {
            new DatabaseAccess().execute(UPDATE_HISTORY);
        }else{
            startMapsActivity();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }


    //TODO throw this in its own class and send a Handler message when complete
    class DatabaseAccess extends AsyncTask<String,Void,String> {
        private final String[] projection = {HistoryEntry._ID, HistoryEntry.START_STATION,HistoryEntry.START_LAT,HistoryEntry.START_LON,
                HistoryEntry.END_STATION,HistoryEntry.END_LAT,HistoryEntry.END_LON,HistoryEntry.DATE};
        private final String sortOrder = HistoryEntry._ID + " DESC";

        private HistoryDbHelper dbHelper;
        private SQLiteDatabase db;

        public DatabaseAccess(){
            dbHelper = new HistoryDbHelper(HistoryActivity.this);
        }


        @Override
        protected void onPreExecute(){
            mDialog = ProgressDialog.show(HistoryActivity.this, "Please Wait...", "Accessing history...", true);
        }

        @Override
        protected String doInBackground(String... params) {
            db = dbHelper.getReadableDatabase();

            if(params[0].equals(UPDATE_HISTORY)){
                Calendar cal = Calendar.getInstance();
                String date = cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

                db = dbHelper.getWritableDatabase();
                ContentValues toWrite = new ContentValues();
                toWrite.put(HistoryEntry.START_STATION, mCurrentSelection.starting);
                toWrite.put(HistoryEntry.START_LAT, mCurrentSelection.startLat);
                toWrite.put(HistoryEntry.START_LON, mCurrentSelection.startLon);
                toWrite.put(HistoryEntry.END_STATION, mCurrentSelection.ending);
                toWrite.put(HistoryEntry.END_LAT, mCurrentSelection.endLat);
                toWrite.put(HistoryEntry.END_LON, mCurrentSelection.endLon);
                toWrite.put(HistoryEntry.DATE, date);
                db.insert(HistoryEntry.TABLE_NAME, null, toWrite);
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            Cursor cursor;
            String id, startingStation, startLat, startLon, endingStation, endLat, endLon, date;

            mDialog.cancel();

            if(result.equals(GET_HISTORY)) {
                cursor = db.query(HistoryEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);


                if(cursor != null && cursor.moveToFirst()){
                    do {
                        id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry._ID));
                        startingStation = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_STATION));
                        startLat = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_LAT));
                        startLon = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_LON));
                        endingStation = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_STATION));
                        endLat = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_LAT));
                        endLon = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_LON));
                        date = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.DATE));

                        HistoryActivity.this.mHistoryCoords.add(new SixTuple(startingStation, endingStation, startLat, startLon, endLat, endLon));

                        mMainLayout.addView(new HistoryView(HistoryActivity.this, "Start: " + startingStation, "End: " + endingStation,
                                "Date: " + date, id, HistoryActivity.this.mHandler));
                        mMainLayout.addView(new ShadowView(HistoryActivity.this));
                    } while (cursor.moveToNext());
                }else{
                    mMainLayout.addView(new IncidentView(HistoryActivity.this,"No past trips found!"));
                    mMainLayout.addView(new ShadowView(HistoryActivity.this));
                }

            }else if(result.equals(CLEAR_HISTORY)){
                dbHelper.recreateTable(db);
                Toast.makeText(HistoryActivity.this, "History cleared", Toast.LENGTH_SHORT).show();

                mMainLayout.removeAllViews();
                mMainLayout.addView(new IncidentView(HistoryActivity.this,"No past trips found!"));
                mMainLayout.addView(new ShadowView(HistoryActivity.this));

            }else if(result.equals(UPDATE_HISTORY)){
                startMapsActivity();
            }
        }
    }

    class SixTuple {
        String starting, ending;
        double startLat, startLon, endLat, endLon;

        public SixTuple(String starting, String ending, String startLat, String startLon, String endLat, String endLon){
            this.starting = starting;
            this.ending = ending;
            this.startLat = Double.parseDouble(startLat);
            this.startLon = Double.parseDouble(startLon);
            this.endLat = Double.parseDouble(endLat);
            this.endLon = Double.parseDouble(endLon);

            Log.d(MainActivity.TAG, "Four tuple {" + this.startLat + ", " + this.startLon + ", " + this.endLat + ", " + this.endLon + "}");
        };
    }
}
