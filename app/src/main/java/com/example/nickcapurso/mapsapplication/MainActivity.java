package com.example.nickcapurso.mapsapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Created by nickcapurso on 3/2/15.
 */
public class MainActivity extends Activity {
    public static final String TAG = "MetroPlanner";

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
                break;
            case R.id.btnPlanTrip:
                Log.d(TAG, "btnPlanTrip");
                break;
            case R.id.btnTripHistory:
                Log.d(TAG, "btnTripHistory");
                break;
            case R.id.btnDelays:
                Log.d(TAG, "btnDelays");
                break;
            case R.id.btnMetroMap:
                Log.d(TAG, "btnMetroMap");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://planitmetro.com/wp-content/uploads/2013/09/Final-Map-without-addresses-07-13.png")));
                break;
        }
    }
}
