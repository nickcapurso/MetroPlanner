package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class MainActivity extends Activity {
    public static final String TAG = "MetroPlanner";
    private static int PADDING_SMALL;

    private static final String GEOCODING_URL = "http://maps.google.com/maps/api/geocode/json";
    private static final String GOOGLE_API_KEY = "AIzaSyCeeioXvbj7KXqeBGAtyFKiz_2Z9Y5txrQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PADDING_SMALL = (int) getResources().getDimension(R.dimen.padding_small);
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

    private void showAddressPicker(String addressJSON){
        final AlertDialog.Builder addressPicker = new AlertDialog.Builder(this);

        JSONObject jsonParser = null;
        JSONArray jsonArray = null;
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.activity_incidents, null);
        LinearLayout mainLayout = (LinearLayout)layout.findViewById(R.id.incidentLayout);


        try {
            jsonParser = new JSONObject(addressJSON);

            if(!checkStatusCode(jsonParser.getString("status")))
                return;

            jsonArray = jsonParser.getJSONArray("results");
            Log.d(TAG, "Elements: " + jsonArray.length());

            for(int i = 0; i < jsonArray.length(); i++){
                final String address = jsonArray.getJSONObject(i).getString("formatted_address");
                LinearLayout container = getAddressContainer();
                container.addView(getAddressTextView(address));

                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, address);
                    }
                });

                mainLayout.addView(container);
                mainLayout.addView(new ShadowView(this));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            showToast("Error performing address lookup");
        }

        addressPicker.setView(layout);
        addressPicker.setTitle("Select Closest Match");
        addressPicker.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        mainLayout.invalidate();
        addressPicker.show();
        /*
        try {
            Log.d(TAG, "Elements: " + jsonArray.getJSONObject(0).getJSONArray(("address_components")).getJSONObject(0).getString("long_name"));
            Log.d(TAG, "Elements: " + jsonArray.getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */
    }

    private boolean checkStatusCode(String status){
        Log.d(TAG, "Checking status:"+status);
        if(status.equals("OK")){
            return true;
        }else if(status.equals("ZERO_RESULTS")){
            showToast("No matches found for this address or location");
        }else if(status.equals("OVER_QUERY_LIMIT")){
            showToast("Reached the limit for address lookup, please try again later");
        }else if(status.equals("REQUEST_DENIED")){
            showToast("Address lookup request denied by server, please try again later");
        }else if(status.equals("INVALID_REQUEST")){
            showToast("Invalid address lookup request");
        }else if(status.equals("UNKNOWN_ERROR")){
            showToast("Error performing address lookup, please try again");
        }
        return false;
    }

    private LinearLayout getAddressContainer(){
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        container.setBackgroundColor(getResources().getColor(R.color.white));
        container.setLayoutParams(parentParams);
        container.setBackground(getResources().getDrawable(R.drawable.white_button));
        return container;
    }

    private TextView getAddressTextView(String address){
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams incidentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setText(address);
        tv.setLayoutParams(incidentParams);
        return tv;
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
