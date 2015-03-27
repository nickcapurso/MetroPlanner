package com.example.nickcapurso.mapsapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity {
    private static final int ONE_SECOND = 1000;

    private boolean mRoutePlanned;
    private double mCenterLatitude, mCenterLongitude;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Intent mIntent;
    private AddressInfo mStartingAddr;
    private AddressInfo mEndingAddr;
    private PlanningModule mPlanningModule;
    private ProgressDialog mDialog;
    private ArrayList<MetroPath> mPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        mStartingAddr = (AddressInfo) getIntent().getParcelableExtra("startingAddr");
        mEndingAddr = (AddressInfo) getIntent().getParcelableExtra("endingAddr");
        mPaths = new ArrayList<MetroPath>();
        mCenterLatitude = mCenterLongitude = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(!mRoutePlanned){
            mPlanningModule = new PlanningModule(mStartingAddr, mEndingAddr, mHandler);
            showProgessDialog();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(HandlerCodes.START_PLANNING_MODULE), ONE_SECOND);

        }
    }

    private void showProgessDialog(){
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(false);
        mDialog.setMax(6);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setTitle("Please Wait...");
        mDialog.setMessage("Planning Metro Route...");
        mDialog.show();
    }

    private void drawPaths(){
        for(MetroPath path : mPaths){
            if(path.sameLine){
                mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(path.startStation.latitude, path.startStation.longitude),
                        new LatLng(path.endStation.latitude, path.endStation.longitude))
                        .width(5)
                        .color(getColor(path.startLine)));

                mCenterLatitude += path.startStation.latitude + path.endStation.latitude;
                mCenterLongitude += path.endStation.longitude + path.endStation.longitude;
            }else{
                mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(path.startStation.latitude, path.startStation.longitude),
                                new LatLng(path.intersectionStation.latitude, path.intersectionStation.longitude))
                        .width(5)
                        .color(getColor(path.startLine)));

                mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(path.intersectionStation.latitude, path.intersectionStation.longitude),
                                new LatLng(path.endStation.latitude, path.endStation.longitude))
                        .width(5)
                        .color(getColor(path.endLine)));
            }
        }

        mCenterLatitude = mCenterLatitude / (2*mPaths.size());
        mCenterLongitude = mCenterLongitude / (2*mPaths.size());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mCenterLatitude, mCenterLongitude)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(12));
    }

    public int getColor(String line){
        int color;
        //RD, BL, YL, OR, GR, or SV
        switch(line){
            case "RD":
                color = Color.RED;
                break;
            case "BL":
                color = Color.BLUE;
                break;
            case "YL":
                color = Color.YELLOW;
                break;
            case "OR":
                //TODO
                color = Color.BLACK;
                break;
            case "GR":
                color = Color.GREEN;
                break;
            case "SV":
                color = Color.GRAY;
                break;
            default:
                color = Color.BLACK;
        }
        return color;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                //setUpMap();
               /* Polyline startLine = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(38.9006980092, -77.050277739), new LatLng(38.9050688072, -76.8420375202))
                        .width(5)
                        .color(Color.BLUE));
                        */
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            Log.d(MainActivity.TAG, "MapsActivity, message received (" + message.what + ")");
            switch(message.what){
                case HandlerCodes.UPDATE_PROGRESS:
                    mDialog.incrementProgressBy((Integer)message.obj);
//                    if(mDialog.getProgress() == 6) {
//                        mRoutePlanned = true;
//                        mDialog.cancel();
//                    }
                    break;
                case HandlerCodes.START_PLANNING_MODULE:
                    mPlanningModule.start();
                    break;
                case HandlerCodes.PLANNING_MODULE_DONE:
                    mRoutePlanned = true;
                    mPaths = (ArrayList<MetroPath>)message.obj;
                    drawPaths();
                    mDialog.cancel();
                    break;
                case HandlerCodes.PLANNING_MODULE_ERR:
                    mDialog.cancel();
                    Toast.makeText(MapsActivity.this, (String)message.obj, Toast.LENGTH_LONG).show();
                    break;
                case HandlerCodes.TIMEOUT:
                    Toast.makeText(MapsActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
}
