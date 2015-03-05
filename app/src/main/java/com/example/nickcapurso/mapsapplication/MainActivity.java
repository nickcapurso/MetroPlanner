package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class MainActivity extends Activity {
    public static final String TAG = "MetroPlanner";

    private static final String GEOCODING_URL = "http://maps.google.com/maps/api/geocode/json";
    private static final String GOOGLE_API_KEY = "AIzaSyCeeioXvbj7KXqeBGAtyFKiz_2Z9Y5txrQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View v){
        int id = v.getId();
        switch(id){
            case R.id.btnSearch:
                Log.d(TAG, "btnSearch");
                EditText tvDestination = (EditText)findViewById(R.id.etDestination);
                String input = tvDestination.getText().toString();

                if(input.equals("")){
                    Toast.makeText(this, "Please enter an address or location", Toast.LENGTH_SHORT).show();
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

    private void geocodeFetched(String result){
        Log.d(TAG, "Results received");
    }

    private void showAddressPicker(String addressJSON){
        JSONObject jsonParser = null;
        JSONArray jsonArray = null;

        final Dialog addressPicker = new Dialog(this);
        addressPicker.setContentView(R.layout.activity_incidents);
        addressPicker.setTitle("Select Closest Match");

        try {
            jsonParser = new JSONObject(addressJSON);
            jsonArray = jsonParser.getJSONArray("results");
            Log.d(TAG, "Elements: " + jsonArray.length());

        } catch (JSONException e) {
            e.printStackTrace();
        }

            try {
                Log.d(TAG, "Elements: " + jsonArray.getJSONObject(0).getJSONArray(("address_components")).getJSONObject(0).getString("long_name"));
                Log.d(TAG, "Elements: " + jsonArray.getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){
                case JSONFetcher.FETCH_COMPLETE:
                    showAddressPicker((String) message.obj);
                    break;
            }
        }
    };
}
