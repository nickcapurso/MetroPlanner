package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

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
        ProgressDialog.show(this, "Please Wait...", "Loading Incidents...", true);
    }

    private class FetchIncidents extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            for(int i=0;i<5;i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
