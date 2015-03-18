package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class IncidentsActivity extends Activity {
    private static final String WMATA_API_KEY = "kfgpmgvfgacx98de9q3xazww";
    private static final String RAIL_INCIDENTS_URL = "https://api.wmata.com/Incidents.svc/json/Incidents";
    private static final String BUS_INCIDENTS_URL = "https://api.wmata.com/Incidents.svc/json/BusIncidents";

    private LinearLayout mMainLayout;

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
        new JSONFetcher(this, mHandler, "Loading Incidents...").execute(RAIL_INCIDENTS_URL, "api_key", WMATA_API_KEY);
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
            return;
        }

        Log.d(MainActivity.TAG, "Number of incidents: " + incidentsList.length());
        if(incidentsList.length() == 0){
            mMainLayout.addView(new IncidentView(this,"No incident to report",""));
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
                mMainLayout.addView(new IncidentView(this, linesAffected, description));
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
                    incidentsFetched((String)message.obj);
                    break;
            }
        }
    };

}
