package com.example.nickcapurso.mapsapplication;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity used to display any metro incidents to the user.
 */
public class IncidentsActivity extends ActionBarActivity {
    /**
     * Set to indicate that incidents have been fetched (and thus, does not need to be
     * repeated when onResume is called again.
     */
    private boolean mFetchComplete;

    /**
     * The main layout in which to place multiple IncidentViews
     */
    private LinearLayout mMainLayout;

    /**
     * Reference to a "loading" dialog
     */
    private ProgressDialog mDialog;

    /**
     * Initialize any variables and views
     * @param savedInstanceState Unused - rotations disabled.
     */
   @Override
   protected void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_incidents);

       mMainLayout = (LinearLayout)findViewById(R.id.incidentLayout);
   }

    /**
     * The actual query for metro incidents is done during the onResume callback.
     */
    @Override
    public void onResume(){
        super.onResume();
        if(!mFetchComplete) {
            mMainLayout.removeAllViews();
            mDialog = ProgressDialog.show(this, "Please Wait...", "Fetching incidents...", true);
            new JSONFetcher(mHandler).execute(API_URLS.RAIL_INCIDENTS, "api_key", API_URLS.WMATA_API_KEY);
        }
    }

    /**
     * Upon return of the network query, create an IncidentView for each incident found (or
     * display an error message if there was a problem fetching metro incidents).
     * @param incidentsJSON
     */
    private void incidentsFetched(String incidentsJSON){
        JSONObject jsonParser;
        JSONArray incidentsList;
        String description = "";

        try {
            jsonParser = new JSONObject(incidentsJSON);
            incidentsList = jsonParser.getJSONArray("Incidents");
        } catch (JSONException e) {
            Log.d(MainActivity.TAG, "Error parsing JSON");e.printStackTrace();
            Toast.makeText(IncidentsActivity.this, "Error fetching incidents list.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(MainActivity.TAG, "Number of incidents: " + incidentsList.length());

        //If there are no incidents, display an IncidentView with a different message
        if(incidentsList.length() == 0){
            Log.d(MainActivity.TAG, "No incidents");
            mMainLayout.addView(new IncidentView(this, "No incidents to report"));
            mMainLayout.addView(new ShadowView(this));
        }else{
            //For each incident, create a new IncidentView
            for(int i = 0; i < incidentsList.length(); i++){
                try {
                    description = incidentsList.getJSONObject(i).getString("Description");
                } catch (JSONException e) {
                    Log.d(MainActivity.TAG, "Error parsing JSON");
                    mHandler.sendMessage(mHandler.obtainMessage(HandlerCodes.JSON_FETCH_ERR));
                    e.printStackTrace();
                    return;
                }
                mMainLayout.addView(new IncidentView(this, description));
                mMainLayout.addView(new ShadowView(this));
            }
        }

        mMainLayout.invalidate();
    }

    /**
     * Receives messages from other entities. In this case, from the JSONFetcher upon the success
     * or failure of a network query and also from the NetworkTimeout if a timeout occurs.
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    mDialog.cancel();

                    if((String)message.obj == null){
                        Toast.makeText(IncidentsActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mFetchComplete = true;
                    incidentsFetched((String)message.obj);
                    break;
                case HandlerCodes.TIMEOUT:
                    Toast.makeText(IncidentsActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                    if(mDialog.isShowing())
                        mDialog.cancel();
                    break;
                case HandlerCodes.JSON_FETCH_ERR:
                    Toast.makeText(IncidentsActivity.this, "Error receiving data from WMATA", Toast.LENGTH_LONG).show();
                    if(mDialog.isShowing())
                        mDialog.cancel();
                    break;
            }
        }
    };

}
