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
    private static final String GEOCODING_URL = "http://maps.google.com/maps/api/geocode/json";
    private static final String GOOGLE_API_KEY = "AIzaSyCeeioXvbj7KXqeBGAtyFKiz_2Z9Y5txrQ";
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
                    showToast("Please enter an address or location");
                    return;
                }

                new JSONFetcher(this, mHandler, "Finding address...").execute(GEOCODING_URL, "address", input);
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

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    AddressPicker addressPicker = new AddressPicker(MainActivity.this, this);
                    addressPicker.show((String) message.obj);
                    break;
                case HandlerCodes.ADDRESS_CHOSEN:
                    mStartingAddr = (AddressInfo)message.obj;
                    mDialog = ProgressDialog.show(MainActivity.this, "Please Wait...", "Obtaining Location Fix",true);
                    mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, MainActivity.this, null);
                    break;
            }
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        mDialog.cancel();
        Log.d(TAG, "Obtained location - lat: " + location.getLatitude() + ", lng: " + location.getLongitude());

        //TODO start planning module. At this point, we have both the source and destination address.
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
