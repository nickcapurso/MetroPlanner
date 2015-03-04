package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class IncidentsActivity extends Activity {
   @Override
   protected void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_incidents);
   }

    @Override
    public void onResume(){
        super.onResume();
        new FetchIncidents(this).execute("https://api.wmata.com/Incidents.svc/json/Incidents", "kfgpmgvfgacx98de9q3xazww");
    }

    private class FetchIncidents extends AsyncTask<String, Void, String> {
        Context context;
        ProgressDialog dialog;

        public FetchIncidents(Context context){
            this.context = context;
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
        protected void onPostExecute(String incidents) {
            Log.d(MainActivity.TAG, "Fetch complete: " + incidents);
            dialog.cancel();

        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(context, "Please Wait...", "Loading Incidents...", true);
        }
    }
}
