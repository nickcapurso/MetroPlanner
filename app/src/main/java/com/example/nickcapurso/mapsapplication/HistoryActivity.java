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
 * Activity which allows a user to view past metro trips they have planned (i.e. gotten metro
 * directions for). A user is able to select one of the history items to view the trip/directions
 * again on a map (via the Maps Activity).
 */
public class HistoryActivity extends ActionBarActivity implements LocationListener{

    //The following are constants to be used with the DatabaseAccess class to determine
    //the course of action
    private static final String GET_HISTORY = "getHistory";
    private static final String CLEAR_HISTORY = "clearHistory";
    private static final String UPDATE_HISTORY = "updateHistory";

    /**
     * A key associated with a value in the preferences file - determines whether or
     * not a re-searched history item is re-added to the top of the history.
     */
    private static final String RESAVE_KEY = "resave";


    /**
     * Constant address "placeholder" for history entries whose starting address
     * was derived by the GPS or Network location provider (via the quick search option)
     */
    public static final String CURRENT_LOCATION = "Current Location";

    /**
     * Determines whether or not a re-searched history item is re-added to the top of the history.
     * Is read from / written to the preferences file.
     */
    private boolean mResave;

    /**
     * Holds the individual contents of each history entry read from the database. Used
     * to send the starting/ending coordinates to the MapsActivity when the user selects a history
     * item to re-search.
     */
    private ArrayList<SixTuple> mHistoryCoords = new ArrayList<SixTuple>();

    /**
     * A reference to one of the SixTuple objects within mHistoryCoords. Corresponds to the entry
     * that the user clicked (used to re-add the history entry to the top of the history if the option
     * is enabled to do so).
     */
    private SixTuple mCurrentSelection;

    /**
     * Reference to the LinearLayout to fill with HistoryViews
     */
    private LinearLayout mMainLayout;

    /**
     * Reference to the progress dialog that displays while the database is being accessed
     */
    private ProgressDialog mDialog;

    /**
     * Location Manager reference to get network/gps location. Used when the user opts to re-search
     * a history entry whose starting address was the "Current Location"
     */
    private LocationManager mLocationManager;

    /**
     * Timeout for location requests
     */
    private NetworkTimeout mTimer;

    /**
     * Shared preferences file to store the mResave boolean
     */
    private SharedPreferences mPrefs;

    /**
     * Editor for the preferences file
     */
    private SharedPreferences.Editor mPrefsEditor;


    /**
     * Initialize any variables and views
     * @param savedInstanceState Unused - rotations disabled.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        mMainLayout = (LinearLayout)findViewById(R.id.historyLayout);

        //Set up preferences file
        mPrefs = getPreferences(Context.MODE_PRIVATE);
        mPrefsEditor = mPrefs.edit();

        //Read history entry re-adding option from the preferences file (defaults to false)
        mResave = mPrefs.getBoolean(RESAVE_KEY, false);
    }

    /**
     * Initialize the options menu on the ActionBar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_history, menu);

        //CheckBox in the ActionBar doesn't check itself automatically - need
        //to do this manually.
        menu.findItem(R.id.history_resave).setChecked(mResave);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Processes click events for items on the ActionBar (either clears the history
     * or sets the history re-saving option)
     * @param item The clicked menu item on the ActionBar.
     * @return
     */
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

    /**
     * OnResume the MainLayout is cleared and the Database is re-queried to set up the
     * HistoryViews. Mainly, this allows the user to see a history item re-saved when hit the Back
     * button to return to this activity (given they had that option enabled)
     */
    @Override
    public void onResume(){
        super.onResume();
        mMainLayout.removeAllViews();
        new DatabaseAccess().execute(GET_HISTORY);
    }

    /**
     * Creates and executes the intent to start the MapsActivity (passing it the
     * starting and ending address/coordinates)
     */
    private void startMapsActivity(){
        Intent startMaps = new Intent(HistoryActivity.this, MapsActivity.class);
        startMaps.putExtra("startingAddr", new AddressInfo(mCurrentSelection.starting, mCurrentSelection.startLat, mCurrentSelection.startLon));
        startMaps.putExtra("endingAddr", new AddressInfo(mCurrentSelection.ending, mCurrentSelection.endLat, mCurrentSelection.endLon));
        startActivity(startMaps);
    }

    /**
     * Commites changes to the preferences file (the resave option)
     */
    private void savePreferences(){
        mPrefsEditor.putBoolean(RESAVE_KEY, mResave);
        mPrefsEditor.commit();
    }

    /**
     * Receives messages from either the NetworkTimeout (GPS/Network location request taking too long)
     * or from one of the HistoryViews when one is clicked.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case HandlerCodes.HISTORY_ENTRY_CHOSEN:
                    int index = mHistoryCoords.size() - ((int)message.obj);
                    mCurrentSelection = mHistoryCoords.get(index);

                    //If the starting address is "Current Location" then the GPS/Network location provider
                    //needs to be invoked to get a location fix.
                    if(mCurrentSelection.starting.equals(CURRENT_LOCATION)){
                        mDialog = ProgressDialog.show(HistoryActivity.this, "Please Wait...", "Obtaining Location Fix", true);
                        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, HistoryActivity.this, null);
                        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, HistoryActivity.this, null);
                        mTimer = new NetworkTimeout(mHandler);
                        mTimer.start();

                    //Else, simply prepare to start the MapsActivity (and optionally rewrite the
                    //clicked history item to the database)
                    }else{
                        if(mResave) {
                            new DatabaseAccess().execute(UPDATE_HISTORY);
                        }else{
                            startMapsActivity();
                        }
                    }
                    Log.d(MainActivity.TAG, "Chosen entry: " + index);
                    break;

                //Location fetch taking too long
                case HandlerCodes.TIMEOUT:
                    Toast.makeText(HistoryActivity.this, "Unable to get location fix.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /**
     * Get the longitude and latitude coordinates from the found location and prepare to start the
     * MapsActivity (and optionally rewrite the clicked history item to the database)
     * @param location
     */
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


    /**
     * Class to handle accesses to the trip history database. It may be possible to combine this with
     * the MainActivity's DatabaseAccess class (similar to how the JSONFetcher is done).
     *
     * Currently, the HistoryActivity's DatabaseAccess class can either update, clear, or get all history
     * entries.
     */
    class DatabaseAccess extends AsyncTask<String,Void,String> {
        /**
         * Table column projects for the SQL query
         */
        private final String[] projection = {HistoryEntry._ID, HistoryEntry.START_STATION,HistoryEntry.START_LAT,HistoryEntry.START_LON,
                HistoryEntry.END_STATION,HistoryEntry.END_LAT,HistoryEntry.END_LON,HistoryEntry.DATE};

        /**
         * Sorting order for the SQL query. The results are sorting by their Android-generated ID in
         * descending order (i.e. the newest history entry will be at the top, which is desired)
         */
        private final String sortOrder = HistoryEntry._ID + " DESC";

        private HistoryDbHelper dbHelper;
        private SQLiteDatabase db;

        public DatabaseAccess(){
            dbHelper = new HistoryDbHelper(HistoryActivity.this);
        }

        /**
         * Starts a "loading" dialog
         */
        @Override
        protected void onPreExecute(){
            mDialog = ProgressDialog.show(HistoryActivity.this, "Please Wait...", "Accessing history...", true);
        }

        /**
         * If the action (passed in params) is UPDATE_HISTORY, then the currently selected history
         * item is written back to the history (i.e. it will become to newest item)
         * @param params The database action to perform (update, delete, get all)
         * @return Passes the database action to onPostExecute
         */
        @Override
        protected String doInBackground(String... params) {
            db = dbHelper.getReadableDatabase();

            if(params[0].equals(UPDATE_HISTORY)){
                Calendar cal = Calendar.getInstance();
                String date = (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

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

        /**
         * If the database action is to retrieve all history items or clear the history, these actions
         * are done on the UI thread because they involve manipulation of HistoryViews.
         * @param result Database action passed on from doInBackground
         */
        @Override
        protected void onPostExecute(String result) {
            Cursor cursor;

            //Temporary String variables used to extract each piece of information from a history entry
            String id, startingStation, startLat, startLon, endingStation, endLat, endLon, date;

            mDialog.cancel();

            if(result.equals(GET_HISTORY)) {
                cursor = db.query(HistoryEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);


                if(cursor != null && cursor.moveToFirst()){
                    do {
                        //Retrieve the individual data pieces from each history entry
                        id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry._ID));
                        startingStation = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_STATION));
                        startLat = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_LAT));
                        startLon = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.START_LON));
                        endingStation = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_STATION));
                        endLat = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_LAT));
                        endLon = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.END_LON));
                        date = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.DATE));

                        //Add the history entry's address & coordinates to the coordinates array
                        HistoryActivity.this.mHistoryCoords.add(new SixTuple(startingStation, endingStation, startLat, startLon, endLat, endLon));

                        //Create and add a new HistoryView for the new history entry
                        mMainLayout.addView(new HistoryView(HistoryActivity.this, "Start: " + startingStation, "End: " + endingStation,
                                "Date: " + date, id, HistoryActivity.this.mHandler));
                        mMainLayout.addView(new ShadowView(HistoryActivity.this));
                    } while (cursor.moveToNext());

                //No prior history
                }else{
                    mMainLayout.addView(new IncidentView(HistoryActivity.this,"No past trips found!"));
                    mMainLayout.addView(new ShadowView(HistoryActivity.this));
                }

            //Uses the HistoryDbHelper class to clear and recreate the history table and
            //clear all HistoryViews from the main layout.
            }else if(result.equals(CLEAR_HISTORY)){
                dbHelper.recreateTable(db);
                Toast.makeText(HistoryActivity.this, "History cleared", Toast.LENGTH_SHORT).show();

                mMainLayout.removeAllViews();

                //Create a new HistoryView to display "No past trips found!"
                mMainLayout.addView(new IncidentView(HistoryActivity.this,"No past trips found!"));
                mMainLayout.addView(new ShadowView(HistoryActivity.this));

            //If the action was to update the history, then start the MapsActivity
            }else if(result.equals(UPDATE_HISTORY)){
                startMapsActivity();
            }
        }
    }

    /**
     * Container for entries read from the history database
     */
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
