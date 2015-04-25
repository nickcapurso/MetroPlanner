package com.example.nickcapurso.mapsapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
 * Displays a set of addresses for the user to pick from - this set is generated from sending
 * the user's input (starting or ending address) to the Google Places API and getting a JSON
 * object containing possible matching locations (these actions are done in the MainActivity).
 */
public class AddressPicker {
    /**
     * Padding for each view the AddressPicker creates for each address
     */
    private static int PADDING_SMALL;

    /**
     * Context of the parent activity to create views
     */
    private final Context mContext;

    /**
     * Reference to the client (MainActivity)'s handler to send a message to when an
     * address is selected
     */
    private final Handler mClientHandler;

    /**
     * The actual "AddressPicker" dialog is built from an AlertDialog
     */
    private AlertDialog mAlertDialog;

    /**
     * Title of the dialog
     */
    private String mTitle = "";

    public AddressPicker(Context context, Handler clientHandler){
        mContext = context;
        mClientHandler = clientHandler;
        PADDING_SMALL = (int) mContext.getResources().getDimension(R.dimen.padding_small);
    }

    public AddressPicker(Context context, Handler clientHandler, String title){
        mContext = context;
        mClientHandler = clientHandler;
        mTitle = title;
        PADDING_SMALL = (int) mContext.getResources().getDimension(R.dimen.padding_small);
    }

    /**
     * Given a JSON object containing addresses to display, this method creates a custom view
     * for each address to display it to the user.
     * @param addressJSON Result from the Google Places API
     */
    public void show(String addressJSON){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        JSONObject jsonParser = null;
        JSONArray jsonArray = null;

        //Initialize the main layout of the dialog
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.activity_incidents, null);
        LinearLayout mainLayout = (LinearLayout)layout.findViewById(R.id.incidentLayout);

        try {
            jsonParser = new JSONObject(addressJSON);

            //Make sure the error status code isn't set
            if(!checkStatusCode(jsonParser.getString("status"))) {
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.ADDRESS_PICKER_ERR));
                return;
            }

            jsonArray = jsonParser.getJSONArray("results");
            Log.d(MainActivity.TAG, "Elements: " + jsonArray.length());

            //For each address, create a custom view to display it to the user
            for(int i = 0; i < jsonArray.length(); i++){
                final JSONObject addressObj = jsonArray.getJSONObject(i);
                final String address = addressObj.getString("formatted_address");
                final double latitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                final double longitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                //Call method to get a formatted layout for the address
                LinearLayout container = getAddressContainer();
                container.addView(getAddressTextView(address));

                //When the user clicks an address, send a message to the MainActivity include its name, latitude, and longitude
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(MainActivity.TAG, address + ", lat: " + latitude + ", lng: " + longitude);
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.ADDRESS_CHOSEN, new AddressInfo(address,latitude,longitude)));
                        mAlertDialog.cancel();
                    }
                });

                mainLayout.addView(container);
                mainLayout.addView(new ShadowView(mContext));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.JSON_FETCH_ERR));
            showToast("Error performing address lookup");
        }

        dialogBuilder.setView(layout);
        dialogBuilder.setTitle(mTitle.equals("") ? "Select Closest Match" : mTitle);

        //Cancel button closes the dialog
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.ADDRESS_PICKER_CANCELLED));
                dialog.cancel();
            }
        });
        mainLayout.invalidate();
        mAlertDialog = dialogBuilder.create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();
    }

    /**
     * Checks the status code for errors. If one exists, show a Toast to display it to the user.
     * @param status
     * @return true if no errors
     */
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

    /**
     * @return a custom view to hold the addresses parsed from the JSON object
     */
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

    /**
     * @param address
     * @return A TextView containing the passed address
     */
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
