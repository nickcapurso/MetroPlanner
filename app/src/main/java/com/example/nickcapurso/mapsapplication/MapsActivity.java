package com.example.nickcapurso.mapsapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity {
    //1-85-147
    private static final int COLOR_BLUE = Color.rgb(4,135,236);
    //190-19-55
    private static final int COLOR_RED = Color.rgb(190,19,55);
    //218-135-7
    private static final int COLOR_ORANGE = Color.rgb(218,135,7);
    //245-212-21
    private static final int COLOR_YELLOW = Color.rgb(245,212,21);
    //0-176-80
    private static final int COLOR_GREEN = Color.rgb(0,176,80);
    //162-164-161
    private static final int COLOR_SILVER = Color.rgb(162,164,161);

    private static final int ONE_SECOND = 1000;
    private static final int LINE_WIDTH = 7;
    private static final int OUTLINE_WIDTH = 4;

    private boolean mRoutePlanned;
    private int mCurrPath = 0;

    private static final String OUTLINE_COLOR = "BLK";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Intent mIntent;
    private AddressInfo mStartingAddr;
    private AddressInfo mEndingAddr;
    private PlanningModule mPlanningModule;
    private ProgressDialog mDialog;
    private ArrayList<MetroPath> mPaths;

    private ImageView ivFirstLegColor, ivSecondLegColor;
    private TextView tvTextLine1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        mStartingAddr = (AddressInfo) getIntent().getParcelableExtra("startingAddr");
        mEndingAddr = (AddressInfo) getIntent().getParcelableExtra("endingAddr");
        mPaths = new ArrayList<MetroPath>();

        ivFirstLegColor = (ImageView)findViewById(R.id.ivFirstLegColor);
        ivSecondLegColor= (ImageView)findViewById(R.id.ivSecondLegColor);
        tvTextLine1 = (TextView)findViewById(R.id.tvTextLine1);
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

    public void onClick(View v){
        switch(v.getId()){
            case R.id.btnPrevPath:
                --mCurrPath;
                if(mCurrPath < 0) mCurrPath = mPaths.size()-1;
                drawPath(mPaths.get(mCurrPath));
                break;
            case R.id.btnNextPath:
                mCurrPath = ++mCurrPath % mPaths.size();
                drawPath(mPaths.get(mCurrPath));
                break;
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

    private void onPlanningModuleDone(){
        drawPath(mPaths.get(0));
    }

    private void drawPath(MetroPath path){
        double numCoordinates, centerLatitude, centerLongitude;
        String desc;
        numCoordinates = centerLatitude = centerLongitude = 0;

        mMap.clear();
        if(path.sameLine){
            ArrayList<StationInfo> firstLeg = path.firstLeg;
            for(int i = 0; i < firstLeg.size(); i++) {
                if(i < firstLeg.size()-1) {
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(firstLeg.get(i).latitude, firstLeg.get(i).longitude),
                                    new LatLng(firstLeg.get(i + 1).latitude, firstLeg.get(i + 1).longitude))
                            .width(LINE_WIDTH + OUTLINE_WIDTH)
                            .color(getColor(OUTLINE_COLOR)));

                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(firstLeg.get(i).latitude, firstLeg.get(i).longitude),
                                    new LatLng(firstLeg.get(i + 1).latitude, firstLeg.get(i + 1).longitude))
                            .width(LINE_WIDTH)
                            .color(getColor(path.startLine)));
                }
                numCoordinates++;
                centerLatitude += firstLeg.get(i).latitude;
                centerLongitude += firstLeg.get(i).longitude;
            }

            mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(path.startIndex).latitude, path.firstLeg.get(path.startIndex).longitude)).title("Start: " + path.firstLeg.get(path.startIndex).name));
            mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(path.endIndex).latitude, path.firstLeg.get(path.endIndex).longitude)).title("End: " + path.firstLeg.get(path.endIndex).name));

            desc = "Start: " + path.firstLeg.get(path.startIndex).name
            + "\nTake: " + getLine(path.startLine) + " line towards " + path.lineTowards.get(path.startLine)
            + "\nEnd: " + path.firstLeg.get(path.endIndex).name;
            Log.d(MainActivity.TAG, "Start at " + path.firstLeg.get(path.startIndex).name +
                    " and take the " + path.startLine +
                    " line towards " + path.lineTowards.get(path.startLine) +
                    " to " + path.firstLeg.get(path.endIndex).name);
        }else{
            ArrayList<StationInfo> drawLongerSection;
            ArrayList<StationInfo> drawShorterSection;
            String longerLine, shorterLine;

            Log.d(MainActivity.TAG, "First color: " + path.startLine + " second: " + path.endLine);
            if(path.firstLeg.size() >= path.secondLeg.size()){
                drawLongerSection = path.firstLeg;
                drawShorterSection = path.secondLeg;
                longerLine = path.startLine;
                shorterLine = path.endLine;
            }else{
                drawLongerSection = path.secondLeg;
                drawShorterSection = path.firstLeg;
                longerLine = path.endLine;
                shorterLine = path.startLine;
            }

            for(int i = 0; i < drawLongerSection.size(); i++){
                numCoordinates++;
                centerLatitude += drawLongerSection.get(i).latitude;
                centerLongitude += drawLongerSection.get(i).longitude;

                if(i < drawLongerSection.size()-1) {
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(drawLongerSection.get(i).latitude, drawLongerSection.get(i).longitude),
                                    new LatLng(drawLongerSection.get(i + 1).latitude, drawLongerSection.get(i + 1).longitude))
                            .width(LINE_WIDTH + OUTLINE_WIDTH)
                            .color(getColor(OUTLINE_COLOR)));

                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(drawLongerSection.get(i).latitude, drawLongerSection.get(i).longitude),
                                    new LatLng(drawLongerSection.get(i + 1).latitude, drawLongerSection.get(i + 1).longitude))
                            .width(LINE_WIDTH)
                            .color(getColor(longerLine)));
                }

                if(i < drawShorterSection.size()){
                    numCoordinates++;
                    centerLatitude += drawShorterSection.get(i).latitude;
                    centerLongitude += drawShorterSection.get(i).longitude;

                    if(i < drawShorterSection.size()-1) {
                        mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(drawShorterSection.get(i).latitude, drawShorterSection.get(i).longitude),
                                        new LatLng(drawShorterSection.get(i + 1).latitude, drawShorterSection.get(i + 1).longitude))
                                .width(LINE_WIDTH + OUTLINE_WIDTH)
                                .color(getColor(OUTLINE_COLOR)));

                        mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(drawShorterSection.get(i).latitude, drawShorterSection.get(i).longitude),
                                        new LatLng(drawShorterSection.get(i + 1).latitude, drawShorterSection.get(i + 1).longitude))
                                .width(LINE_WIDTH)
                                .color(getColor(shorterLine)));
                    }
                }
            }

            int intersection = 0;
            if(path.startIndex == 0)
                intersection = path.firstLeg.size()-1;

            mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(path.startIndex).latitude, path.firstLeg.get(path.startIndex).longitude)).title("Start: " + path.firstLeg.get(path.startIndex).name ));
            mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(intersection).latitude, path.firstLeg.get(intersection).longitude)).title("Transfer: " + path.firstLeg.get(intersection).name));
            mMap.addMarker(new MarkerOptions().position(new LatLng(path.secondLeg.get(path.endIndex).latitude, path.secondLeg.get(path.endIndex).longitude)).title("End: " + path.secondLeg.get(path.endIndex).name));
            desc = "Start: " + path.firstLeg.get(path.startIndex).name
            + "\nTake: " + getLine(path.startLine) + " line towards " + path.lineTowards.get(path.startLine)
            + "\nTransfer: " + path.firstLeg.get(intersection).name
            + "\nTake: " + getLine(path.endLine) + " line towards " + path.lineTowards.get(path.endLine)
            + "\nEnd: " + path.secondLeg.get(path.endIndex).name;
            Log.d(MainActivity.TAG, "Start at " + path.firstLeg.get(path.startIndex).name +
                    " and take the " + getLine(path.startLine) +
                    " line towards " + path.lineTowards.get(path.startLine) +
                    " to " + path.firstLeg.get(intersection).name +
                    ". Transfer to the " + getLine(path.endLine) +
                    " towards " + path.lineTowards.get(path.endLine) +
                    " and end at " + path.secondLeg.get(path.endIndex).name);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(centerLatitude/numCoordinates, centerLongitude/numCoordinates)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(11));
        tvTextLine1.setText(desc);

        if(path.sameLine){
            ivFirstLegColor.setBackgroundColor(getColor(path.startLine));
            ivSecondLegColor.setVisibility(View.INVISIBLE);
        }else{
            ivSecondLegColor.setVisibility(View.VISIBLE);
            ivFirstLegColor.setBackgroundColor(getColor(path.startLine));
            ivSecondLegColor.setBackgroundColor(getColor(path.endLine));
        }
    }

    public String getLine(String code){
        String line = "";

        switch(code){
            case "RD":
                line = "Red";
                break;
            case "BL":
                line = "Blue";
                break;
            case "OR":
                line = "Orange";
                break;
            case "SV":
                line = "Silver";
                break;
            case "YL":
                line = "Yellow";
                break;
            case "GR":
                line = "Green";
                break;
        }
        return line;
    }


    public int getColor(String line){
        int color;
        //RD, BL, YL, OR, GR, or SV
        switch(line){
            case "RD":
                color = COLOR_RED;
                break;
            case "BL":
                color = COLOR_BLUE;
                break;
            case "YL":
                color = COLOR_YELLOW;
                break;
            case "OR":
                color = COLOR_ORANGE;
                break;
            case "GR":
                color = COLOR_GREEN;
                break;
            case "SV":
                color = COLOR_SILVER;
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
                    break;
                case HandlerCodes.START_PLANNING_MODULE:
                    mPlanningModule.start();
                    break;
                case HandlerCodes.PLANNING_MODULE_DONE:
                    mRoutePlanned = true;
                    mPaths = (ArrayList<MetroPath>)message.obj;
                    onPlanningModuleDone();
                    mDialog.cancel();
                    break;
                case HandlerCodes.PLANNING_MODULE_ERR:
                    mDialog.cancel();
                    Toast.makeText(MapsActivity.this, (String)message.obj, Toast.LENGTH_LONG).show();
                    break;
                case HandlerCodes.TIMEOUT:
                    mDialog.cancel();
                    Toast.makeText(MapsActivity.this, "Network error: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
}
