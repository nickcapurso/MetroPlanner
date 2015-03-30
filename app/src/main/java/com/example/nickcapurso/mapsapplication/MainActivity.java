package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by nickcapurso on 3/2/15.
 *
 */
public class MainActivity extends Activity implements LocationListener{
    public static final String TAG = "MetroPlanner";
    private LocationManager mLocationManager;
    private ProgressDialog mDialog;
    private AddressInfo mStartingAddr, mEndingAddr = null;
    private String mEndingAddrString;
    private NetworkTimeout mTimer;

    private boolean mUsingPlanTripDialog, mGettingEndingAddr, mGettingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        for(String s : mLocationManager.getAllProviders()){
            Log.d(TAG, "Provider: " + s);
        }
    }

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

    //TODO if network error or address not found, reset boolean vars
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    mDialog.cancel();

                    if((String)message.obj == null){
                        Toast.makeText(MainActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    AddressPicker addressPicker;
                    if(!mUsingPlanTripDialog)
                        addressPicker = new AddressPicker(MainActivity.this, this);
                    else
                        addressPicker = new AddressPicker(MainActivity.this, this, !mGettingEndingAddr? "Choose Starting Address" : "Choose Ending Address");
                    addressPicker.show((String) message.obj);
                    break;
                case HandlerCodes.ADDRESS_CHOSEN:
                    if(!mUsingPlanTripDialog) {
                        mStartingAddr = (AddressInfo)message.obj;
                        mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Obtaining Location Fix", true);
                        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, MainActivity.this, null);
                        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, MainActivity.this, null);
                        mGettingLocation = true;
                        mTimer = new NetworkTimeout(mHandler);
                        mTimer.start();
                    }else{
                        if(!mGettingEndingAddr) {
                            mStartingAddr = (AddressInfo)message.obj;
                            mGettingEndingAddr = true;
                            mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Finding ending address...", true);
                            new JSONFetcher(mHandler).execute(API_URLS.GEOCODING, "address", mEndingAddrString);
                        }else{
                            mEndingAddr = (AddressInfo)message.obj;
                            mGettingEndingAddr = false;
                            Intent startMaps = new Intent(MainActivity.this, MapsActivity.class);
                            startMaps.putExtra("startingAddr", mStartingAddr);
                            startMaps.putExtra("endingAddr", mEndingAddr);
                            startActivity(startMaps);
                        }
                    }
                    break;
                case HandlerCodes.ADDRESS_PICKER_CANCELLED:
                    mGettingEndingAddr = false;
                    break;
                case HandlerCodes.ADDRESS_PICKER_ERR:
                    mGettingEndingAddr = false;
                    break;
                case HandlerCodes.TIMEOUT:
                    String toastMsg = mGettingLocation ? "Unable to get location fix." : "Network error: please make sure you have networking services enabled.";
                    Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                    if(mDialog.isShowing())
                        mDialog.cancel();
                    mGettingLocation = false;
                    mLocationManager.removeUpdates(MainActivity.this);
                    break;
            }
        }
    };


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

        dialogBuilder.setPositiveButton("Go!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) { }
        });


        final AlertDialog planTripDialog = dialogBuilder.create();
        planTripDialog.show();
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

    @Override
    public void onLocationChanged(Location location) {
        mTimer.cancel();
        mDialog.cancel();
        mLocationManager.removeUpdates(MainActivity.this);
        mGettingLocation = false;
        Log.d(TAG, "Obtained location - lat: " + location.getLatitude() + ", lng: " + location.getLongitude());
        Intent startMaps = new Intent(this, MapsActivity.class);
        startMaps.putExtra("startingAddr", mStartingAddr);
        startMaps.putExtra("endingAddr", new AddressInfo("null", location.getLatitude(), location.getLongitude()));
        startActivity(startMaps);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
