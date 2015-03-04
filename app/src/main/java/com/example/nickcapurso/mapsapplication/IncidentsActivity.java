package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class IncidentsActivity extends Activity {
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
        new FetchIncidents(this).execute("https://api.wmata.com/Incidents.svc/json/Incidents", "kfgpmgvfgacx98de9q3xazww");
    }

    private class FetchIncidents extends AsyncTask<String, Void, String> {
        Context context;
        ProgressDialog dialog;

        public FetchIncidents(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(context, "Please Wait...", "Loading Incidents...", true);
        }

        @Override
        protected String doInBackground(String... args) {
            Log.d(MainActivity.TAG, "FetchIncidents starting...");
            HttpClient httpclient = HttpClients.createDefault();

            try {
                URIBuilder builder = new URIBuilder(args[0]);
                // Specify your subscription key
                builder.setParameter("api_key", args[1]);
                // Specify values for optional parameters, as needed
                // builder.setParameter("Route", "");
                URI uri = builder.build();
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return (EntityUtils.toString(entity));
                }
            } catch (ClientProtocolException e) {
                Log.d(MainActivity.TAG, "FetchIncidents - ClientProtocolException " + e.getStackTrace());
            } catch (IOException e) {
                Log.d(MainActivity.TAG, "FetchIncidents - IOException " + e.getStackTrace());
            } catch (URISyntaxException e) {
                Log.d(MainActivity.TAG, "FetchIncidents - URISyntaxException " + e.getStackTrace());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String incidentsJSON) {
            Log.d(MainActivity.TAG, "Fetch complete: " + incidentsJSON);
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

            if(incidentsList.length() == 0){
                mMainLayout.addView(new IncidentView(context, "No incident to report",""));
                mMainLayout.addView(new ShadowView(context));
            }else{
                for(int i = 0; i < incidentsList.length(); i++){
                    try {
                        linesAffected = incidentsList.getJSONObject(i).getString("LinesAffected");
                        description = incidentsList.getJSONObject(i).getString("Description");
                    } catch (JSONException e) {
                        Log.d(MainActivity.TAG, "Error parsing JSON");e.printStackTrace();
                        return;
                    }
                    mMainLayout.addView(new IncidentView(context, "linesAffected", "description"));
                    mMainLayout.addView(new ShadowView(context));
                }
            }

            mMainLayout.invalidate();
            dialog.cancel();
        }
    }
}
