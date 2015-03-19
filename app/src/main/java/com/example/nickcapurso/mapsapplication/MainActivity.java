package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
    private AddressInfo mStartingAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
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

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    mDialog.cancel();
                    AddressPicker addressPicker = new AddressPicker(MainActivity.this, this);
                    addressPicker.show((String) message.obj);
                    break;
                case HandlerCodes.ADDRESS_CHOSEN:
                    mStartingAddr = (AddressInfo)message.obj;
                    Intent startMaps = new Intent(MainActivity.this, MapsActivity.class);
                    startMaps.putExtra("startingAddr", mStartingAddr);
                    startMaps.putExtra("endingAddr", mStartingAddr);
                    startActivity(startMaps);
                    //mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Obtaining Location Fix",true);
                    //mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, MainActivity.this, null);
                    break;
            }
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        mDialog.cancel();
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
