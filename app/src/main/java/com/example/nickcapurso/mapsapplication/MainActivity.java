package com.example.nickcapurso.mapsapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;

import static com.example.nickcapurso.mapsapplication.HistoryDbInfo.HistoryEntry;

/**
 * The main menu activity of the Metro Planner app. Gateway to the Incident, History, and Maps
 * Activities. Also allows the user to type a destination address in the "Quick Plan" field
 * and uses the GPS/Network location as the source address.
 *
 * Accesses Google Maps and Places APIs through the JSONFetcher class. These are used for
 * address resolution.
 */
public class MainActivity extends ActionBarActivity implements LocationListener{

    /**
     * Tag for logging through LogCat
     */
    public static final String TAG = "MetroPlanner";

    /**
     * Location Manager reference to get network/gps location
     */
    private LocationManager mLocationManager;

    /**
     * Reference to the progress dialog that displays while a network request is being served
     */
    private ProgressDialog mDialog;

    /**
     * AddressInfo objects to hold the name, longitude, and latitude for the starting address
     */
    private AddressInfo mStartingAddr = null;

    /**
     * AddressInfo objects to hold the name, longitude, and latitude for the ending address
     */
    private AddressInfo mEndingAddr = null;

    /**
     * Holds the inputted destination address from the "Plan Trip" dialog, so it can be
     * fetched after the network request returns with the source address
     */
    private String mEndingAddrString;

    /**
     * Timer to timeout when a network or location request is taking too long
     */
    private NetworkTimeout mTimer;

    /**
     * Keeps track of if the user is using the plan trip dialog vs. the quick plan option
     */
    private boolean mUsingPlanTripDialog;

    /**
     * Set to true if the starting address has already been found (thus, the next returning
     * network request corresponds to the ending address)
     */
    private boolean mGettingEndingAddr;

    /**
     * Sets to true if the GPS/Network is currently determining the user's location (used
     * to print the correct error message if mTimer times out)
     */
    private boolean mGettingLocation;


    /**
     * Initialize any variables and views
     * @param savedInstanceState Unused - rotations disabled.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        for(String s : mLocationManager.getAllProviders()){
            Log.d(TAG, "Provider: " + s);
        }
    }


    /**
     * Callback determines the correct action to take depending on which button was pressed.
     * @param v The clicked button
     */
    public void onClick(View v){
        int id = v.getId();
        switch(id){
            case R.id.btnSearch:
                Log.d(TAG, "btnSearch");
                EditText tvDestination = (EditText)findViewById(R.id.etDestination);
                String input = tvDestination.getText().toString();

                if(input.equals("")){
                    Toast.makeText(this, "Please enter an address or location", Toast.LENGTH_LONG).show();
                    return;
                }

                mDialog = ProgressDialog.show(this, "Please Wait...", "Finding address...", true);
                new JSONFetcher(mHandler).execute(API_URLS.GEOCODING, "address", input);
                break;
            case R.id.btnPlanTrip:
                Log.d(TAG, "btnPlanTrip");
                showPlanTripDialog();
                break;
            case R.id.btnTripHistory:
                Log.d(TAG, "btnTripHistory");
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                break;
            case R.id.btnDelays:
                Log.d(TAG, "btnDelays");
                startActivity(new Intent(this, IncidentsActivity.class));
                break;
            case R.id.btnMetroMap:
                Log.d(TAG, "btnMetroMap");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://planitmetro.com/wp-content/uploads/2013/09/Final-Map-without-addresses-07-13.png")));
                break;
        }
    }

    /**
     * Receives messages from other entities and handles them on this Activity's (the UI) thread.
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){

                //JSON results returned from a network query (returned as the object of the message)
                case HandlerCodes.JSON_FETCH_DONE:
                    mDialog.cancel();

                    //No data received
                    if((String)message.obj == null){
                        Toast.makeText(MainActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    //Network queries made from this activity are to Google Places to get an address from inputted data.
                    //Thus, show a "Choose An Address" dialog displaying possible matches.
                    AddressPicker addressPicker;
                    if(!mUsingPlanTripDialog)
                        addressPicker = new AddressPicker(MainActivity.this, this);
                    else
                        addressPicker = new AddressPicker(MainActivity.this, this, !mGettingEndingAddr? "Choose Starting Address" : "Choose Ending Address");
                    addressPicker.show((String) message.obj);
                    break;

                //Message sent from the AddressPicker dialog when the user has selected an address from the list. The message's
                //object contains an AddressInfo object
                case HandlerCodes.ADDRESS_CHOSEN:

                    //If using the quick plan option, then the chosen address will be the ending address. Thus,
                    //start a location quest to get the starting location (uses either GPS or Network location - whichever comes first)
                    if(!mUsingPlanTripDialog) {
                        mEndingAddr = (AddressInfo)message.obj;
                        mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Obtaining Location Fix", true);
                        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, MainActivity.this, null);
                        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, MainActivity.this, null);
                        mGettingLocation = true;

                        //Start timeout
                        mTimer = new NetworkTimeout(mHandler);
                        mTimer.start();

                    //Else, the plan trip option is being used - need to determine which address (starting / ending) is
                    //contained within the message
                    }else{
                        if(!mGettingEndingAddr) {
                            mStartingAddr = (AddressInfo)message.obj;
                            mGettingEndingAddr = true;
                            mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Finding ending address...", true);
                            new JSONFetcher(mHandler).execute(API_URLS.GEOCODING, "address", mEndingAddrString);
                        }else{
                            mEndingAddr = (AddressInfo)message.obj;
                            mGettingEndingAddr = false;
                            new DatabaseAccess().execute();
                        }
                    }
                    break;

                //User cancelled the address picker dialog (reset any relevant booleans)
                case HandlerCodes.ADDRESS_PICKER_CANCELLED:
                    mGettingEndingAddr = false;
                    break;

                //Address picker error - for example, error parsing the JSON result (reset any relevant booleans)
                case HandlerCodes.ADDRESS_PICKER_ERR:
                    mGettingEndingAddr = false;
                    break;

                //Timeout occured (either location fix was taking too long or a network request was taking too long)
                //Error Toast is shown, booleans are reset, and the location request is canceled (if there was one pending)
                case HandlerCodes.TIMEOUT:
                    String toastMsg = mGettingLocation ? "Unable to get location fix." : "Network error: please make sure you have networking services enabled.";
                    Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                    if(mDialog.isShowing())
                        mDialog.cancel();
                    mGettingLocation = false;
                    mLocationManager.removeUpdates(MainActivity.this);
                    break;

                //Network request error - show message to user and reset booleans
                case HandlerCodes.JSON_FETCH_ERR:
                    Toast.makeText(MainActivity.this, "Error receiving data from server", Toast.LENGTH_LONG).show();
                    mGettingEndingAddr = false;
                    if(mDialog.isShowing())
                        mDialog.cancel();
                    break;
            }
        }
    };


    /**
     * Displays a dialog prompting the user to enter in a starting and
     * ending address.
     */
    private void showPlanTripDialog(){
        mUsingPlanTripDialog = true;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_plantrip, null);

        final EditText etTripStart = (EditText)layout.findViewById(R.id.etTripStart);
        final EditText etTripEnd = (EditText)layout.findViewById(R.id.etTripEnd);


        dialogBuilder.setView(layout);
        dialogBuilder.setTitle("Enter Trip Details");
        dialogBuilder.setCancelable(false);
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mGettingEndingAddr = mUsingPlanTripDialog = false;
                dialog.cancel();
            }
        });


        //Creation of the Positive Button ("Go!") is done by supplying a blank constructor for the onClickListener.
        //The onClick listener is added later, after the dialog is shown, to get the desired behavior or leaving
        //the dialog open after the button is pressed.
        dialogBuilder.setPositiveButton("Go!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {     }
        });

        final AlertDialog planTripDialog = dialogBuilder.create();
        planTripDialog.show();

        //When the Positive Button ("GO!") is pressed, a sequence starts to map the inputted strings
        //to actual places using the Google Places API. But, the dialog remains open in case the
        //user wants to get back and correct his input.
        planTripDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Finding starting address...", true);
                mEndingAddrString = etTripEnd.getText().toString();
                new JSONFetcher(mHandler).execute(API_URLS.GEOCODING, "address", etTripStart.getText().toString());
            }
        }));
    }

    /**
     * Called when a location is found by either the GPS or the Network location provider. The
     * provided location is used as the "starting address" (i.e. the latitude and longitude coordinates)
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        mTimer.cancel();
        mDialog.cancel();
        mLocationManager.removeUpdates(MainActivity.this);
        mGettingLocation = false;
        Log.d(TAG, "Obtained location - lat: " + location.getLatitude() + ", lng: " + location.getLongitude());
        mStartingAddr = new AddressInfo(HistoryActivity.CURRENT_LOCATION, location.getLatitude(), location.getLongitude());

        //Execute database task to write both starting and ending addresses to the database
        new DatabaseAccess().execute();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    /**
     * Class to handle accesses to the trip history database
     */
    class DatabaseAccess extends AsyncTask<String,Void,String>{
        HistoryDbHelper dbHelper;
        SQLiteDatabase db;

        public DatabaseAccess(){
            dbHelper = new HistoryDbHelper(MainActivity.this);
        }

        /**
         * Starts a "loading" dialog
         */
        @Override
        protected void onPreExecute(){
            mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Saving to trip history...", true);
            Log.d(TAG, "Begin writing to db");
        }

        /**
         * Database actions are done in the background (to note getWritableDatabase() is suggested
         * to be done in a background thread).
         *
         * Database entries are written in the form:
         *      Android-generated ID, Starting Address, Starting Latitude, Starting Longitude, Ending Address, Ending Latitude, Ending Longitude
         *
         * @param params unused
         * @return unused
         */
        @Override
        protected String doInBackground(String... params) {
            Calendar cal = Calendar.getInstance();
            String date = (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

            db = dbHelper.getWritableDatabase();
            ContentValues toWrite = new ContentValues();
            toWrite.put(HistoryEntry.START_STATION, mStartingAddr.address);
            toWrite.put(HistoryEntry.START_LAT, mStartingAddr.latitude);
            toWrite.put(HistoryEntry.START_LON, mStartingAddr.longitude);
            toWrite.put(HistoryEntry.END_STATION, mEndingAddr.address);
            toWrite.put(HistoryEntry.END_LAT, mEndingAddr.latitude);
            toWrite.put(HistoryEntry.END_LON, mEndingAddr.longitude);
            toWrite.put(HistoryEntry.DATE, date);
            db.insert(HistoryEntry.TABLE_NAME, null, toWrite);
            return null;
        }

        /**
         * Cancel the "loading" dialog and start the MapsActivity (every execution of this class is
         * done prior to launching the MapsActivity, so this can be assumed).
         * @param result unused
         */
        @Override
        protected void onPostExecute(String result) {
            mDialog.cancel();
            Intent startMaps = new Intent(MainActivity.this, MapsActivity.class);
            startMaps.putExtra("startingAddr", mStartingAddr);
            startMaps.putExtra("endingAddr", mEndingAddr);
            startActivity(startMaps);
        }
    }
}
