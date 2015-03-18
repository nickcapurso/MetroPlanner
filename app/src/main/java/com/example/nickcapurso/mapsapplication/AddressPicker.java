package com.example.nickcapurso.mapsapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class AddressPicker {
    private static int PADDING_SMALL;
    private final Context mContext;
    private final LocationManager mLocationManager;
    private final Handler mClientHandler;

    public AddressPicker(Context context, Handler clientHandler){
        mContext = context;
        mClientHandler = clientHandler;
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        PADDING_SMALL = (int) mContext.getResources().getDimension(R.dimen.padding_small);
    }

    public void show(String addressJSON){
        final AlertDialog.Builder addressPicker = new AlertDialog.Builder(mContext);
        JSONObject jsonParser = null;
        JSONArray jsonArray = null;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.activity_incidents, null);
        LinearLayout mainLayout = (LinearLayout)layout.findViewById(R.id.incidentLayout);

        try {
            jsonParser = new JSONObject(addressJSON);

            if(!checkStatusCode(jsonParser.getString("status")))
                return;

            jsonArray = jsonParser.getJSONArray("results");
            Log.d(MainActivity.TAG, "Elements: " + jsonArray.length());

            for(int i = 0; i < jsonArray.length(); i++){
                final JSONObject addressObj = jsonArray.getJSONObject(i);
                final String address = addressObj.getString("formatted_address");
                final double latitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                final double longitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                LinearLayout container = getAddressContainer();
                container.addView(getAddressTextView(address));

                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(MainActivity.TAG, address + ", lat: " + latitude + ", lng: " + longitude);
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.ADDRESS_CHOSEN, new AddressInfo(address,latitude,longitude)));
                    }
                });

                mainLayout.addView(container);
                mainLayout.addView(new ShadowView(mContext));
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
    }

    private boolean checkStatusCode(String status){
        Log.d(MainActivity.TAG, "Checking status:"+status);
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
        LinearLayout container = new LinearLayout(mContext);
        LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        container.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        container.setLayoutParams(parentParams);
        container.setBackground(mContext.getResources().getDrawable(R.drawable.white_button));
        return container;
    }

    private TextView getAddressTextView(String address){
        TextView tv = new TextView(mContext);
        LinearLayout.LayoutParams incidentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setText(address);
        tv.setLayoutParams(incidentParams);
        return tv;
    }

    private void showToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}
