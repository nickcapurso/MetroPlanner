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
 * Created by nickcapurso on 3/2/15.
 */
public class IncidentsActivity extends ActionBarActivity {
    private LinearLayout mMainLayout;
    private ProgressDialog mDialog;

   @Override
   protected void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_incidents);

       mMainLayout = (LinearLayout)findViewById(R.id.incidentLayout);
   }

    @Override
    public void onResume(){
        super.onResume();
        mMainLayout.removeAllViews();
        mDialog = ProgressDialog.show(this, "Please Wait...", "Fetching incidents...", true);
        new JSONFetcher(mHandler).execute(API_URLS.RAIL_INCIDENTS, "api_key", API_URLS.WMATA_API_KEY);
    }

    private void incidentsFetched(String incidentsJSON){
        JSONObject jsonParser;
        JSONArray incidentsList;
        String linesAffected = "", description = "";

        try {
            jsonParser = new JSONObject(incidentsJSON);
            incidentsList = jsonParser.getJSONArray("Incidents");
        } catch (JSONException e) {
            Log.d(MainActivity.TAG, "Error parsing JSON");e.printStackTrace();
            Toast.makeText(IncidentsActivity.this, "Error fetching incidents list.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(MainActivity.TAG, "Number of incidents: " + incidentsList.length());
        if(incidentsList.length() == 0){
            Log.d(MainActivity.TAG, "No incidents");
            mMainLayout.addView(new IncidentView(this, "No incidents to report"));
            mMainLayout.addView(new ShadowView(this));
        }else{
            for(int i = 0; i < incidentsList.length(); i++){
                try {
                    linesAffected = incidentsList.getJSONObject(i).getString("LinesAffected");
                    description = incidentsList.getJSONObject(i).getString("Description");
                } catch (JSONException e) {
                    Log.d(MainActivity.TAG, "Error parsing JSON");e.printStackTrace();
                    return;
                }
                mMainLayout.addView(new IncidentView(this, description));
                mMainLayout.addView(new ShadowView(this));
            }
        }

        mMainLayout.invalidate();
    }

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
