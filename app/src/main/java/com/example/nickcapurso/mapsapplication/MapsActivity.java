package com.example.nickcapurso.mapsapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * The MapsActivity is responsible for initiating the trip-planning sequence (Planning Module) and, when
 * complete, displaying the metro path & textual directions to the user. The metro path is displayed
 * on a Google Maps-supplied map.
 */
public class MapsActivity extends FragmentActivity {
    //RGB color definitions for the different metro lines
    private static final int COLOR_BLUE = Color.rgb(4,135,236);
    private static final int COLOR_RED = Color.rgb(190,19,55);
    private static final int COLOR_ORANGE = Color.rgb(218,135,7);
    private static final int COLOR_YELLOW = Color.rgb(245,212,21);
    private static final int COLOR_GREEN = Color.rgb(0,176,80);
    private static final int COLOR_SILVER = Color.rgb(162,164,161);

    /**
     * Used to calculate DP (for setting View sizes)
     */
    private static float SCALE;

    /**
     * Padding for the edges of the map (in pixels)
     */
    private static final int ZOOM_PADDING = 75;

    /**
     * Height (in DP) for directions layout when only three lines are needed
     */
    private static final int DIRECTIONS_HEIGHT_SMALL = 60;

    /**
     * One second in milliseconds (used for delays)
     */
    private static final int ONE_SECOND = 1000;

    /**
     * Line width of the metro paths drawn on the map
     */
    private static final int LINE_WIDTH = 8;

    /**
     * Line width of the black outline drawn around metro paths
     */
    private static final int OUTLINE_WIDTH = 4;

    /**
     * Outline color
     */
    private static final String OUTLINE_COLOR = "BLK";

    /**
     * Boolean set when the PlanningModule completes
     */
    private boolean mRoutePlanned;

    /**
     * Set while the retry dialog is being shown to the user
     */
    private boolean mRetryShowing;

    /**
     * Boolean set while the PlanningModule is executing
     */
    private boolean mIsPlanning;

    /**
     * The currently displayed metro path
     */
    private int mCurrPath = 0;

    /**
     * The actual maps view
     */
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    /**
     * The starting address (and GPS coords) received from the MainActivity
     */
    private AddressInfo mStartingAddr;

    /**
     * The ending address (and GPS coords) received from the MainActivity
     */
    private AddressInfo mEndingAddr;

    /**
     * Reference to the Planning Module (which does the actual action of the trip planning)
     */
    private PlanningModule mPlanningModule;

    /**
     * Reference to the progress dialog that displays (and updates its progress) as the Planning
     * Module executes
     */
    private ProgressDialog mDialog;

    /**
     * A list of possible metro paths from the starting address to the ending address (as returned
     * by the Planning Module)
     */
    private ArrayList<MetroPath> mPaths;

    /**
     * ImageView to be colored according to the first train the user should take
     */
    private ImageView ivFirstLegColor;

    /**
     * ImageView to be colored according to the second train the user should take (if needed)
     */
    private ImageView ivSecondLegColor;

    /**
     * TextView to contain a text description of the trains the user should take
     */
    private TextView tvDirections;

    /**
     * ArrayList containing the map markers (starting station, intersection station, ending station)
     */
    ArrayList<Marker> mMarkers;

    /**
     * Layout that contains the textual directions
     */
    RelativeLayout mDescriptionLayout;

    /**
     * Initialize any variables and views
     * @param savedInstanceState Unused - rotations disabled.
     */
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
        tvDirections = (TextView)findViewById(R.id.tvTextLine1);

        mMarkers = new ArrayList<Marker>();
        mDescriptionLayout = (RelativeLayout)findViewById(R.id.layout_description);

        SCALE = getResources().getDisplayMetrics().density;
    }

    /**
     * The Planning Module is starting in the activity's onResume. Booleans are set
     * accordingly such that the module does not restart on any subsequent calls to this method
     */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(!mRoutePlanned && !mIsPlanning){
            mIsPlanning = true;
            mPlanningModule = new PlanningModule(mStartingAddr, mEndingAddr, mHandler);
            showProgessDialog();

            //Delay the execution of the planning module by one second (to allow the map to finish initializing)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(HandlerCodes.START_PLANNING_MODULE), ONE_SECOND);
        }
    }

    /**
     * Handles click events for the text description box (allows the user to browse the
     * different possible paths)
     * @param v
     */
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

    /**
     * Set up options for the progress dialog (title, message, max number of progress "ticks", etc.)
     */
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

    /**
     * Callback when the planning module completes
     */
    private void onPlanningModuleDone(){
        drawPath(mPaths.get(0));
    }

    /**
     * Draws a single metro path on the map, centers the camera, and sets the zoom level
     * @param path The metro path to draw
     */
    private void drawPath(MetroPath path){
        double numCoordinates, centerLatitude, centerLongitude;
        String desc;

        mMap.clear();

        //Determine if we are only drawing one leg or two (source/destination on different lines)
        if(path.sameLine){
            ArrayList<StationInfo> firstLeg = path.firstLeg;
            mDescriptionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,(int)(DIRECTIONS_HEIGHT_SMALL * SCALE + 0.5f)));

            //Draw two lines between each station on the line (one for the outline, one for the color)
            for(int i = 0; i < firstLeg.size(); i++) {
                if(i < firstLeg.size()-1) {

                    //Drawing outline
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(firstLeg.get(i).latitude, firstLeg.get(i).longitude),
                                    new LatLng(firstLeg.get(i + 1).latitude, firstLeg.get(i + 1).longitude))
                            .width(LINE_WIDTH + OUTLINE_WIDTH)
                            .color(getColor(OUTLINE_COLOR)));

                    //Drawing colored line
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(firstLeg.get(i).latitude, firstLeg.get(i).longitude),
                                    new LatLng(firstLeg.get(i + 1).latitude, firstLeg.get(i + 1).longitude))
                            .width(LINE_WIDTH)
                            .color(getColor(path.startLine)));
                }
            }

            //Add a map marker for the starting and ending stations
            mMarkers.add(mMap.addMarker(
                    new MarkerOptions().position(new LatLng(path.firstLeg.get(path.startIndex).latitude,
                            path.firstLeg.get(path.startIndex).longitude)).title("Start: " + path.firstLeg.get(path.startIndex).name)));
            mMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(path.endIndex).latitude,
                    path.firstLeg.get(path.endIndex).longitude)).title("End: " + path.firstLeg.get(path.endIndex).name)));

            //Set the textual directions
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

            //Want to draw both paths simultaneously in a single for-loop (thus, need to determine
            //which path is shorter to avoid an IndexOutOfBounds exception)
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

            //Draw both legs of the trip simultaneously (single for-loop)
            for(int i = 0; i < drawLongerSection.size(); i++){

                //Draw the longer leg of the trip
                if(i < drawLongerSection.size()-1) {
                    //Drawing outline
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(drawLongerSection.get(i).latitude, drawLongerSection.get(i).longitude),
                                    new LatLng(drawLongerSection.get(i + 1).latitude, drawLongerSection.get(i + 1).longitude))
                            .width(LINE_WIDTH + OUTLINE_WIDTH)
                            .color(getColor(OUTLINE_COLOR)));

                    //Drawing colored line
                    mMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(drawLongerSection.get(i).latitude, drawLongerSection.get(i).longitude),
                                    new LatLng(drawLongerSection.get(i + 1).latitude, drawLongerSection.get(i + 1).longitude))
                            .width(LINE_WIDTH)
                            .color(getColor(longerLine)));
                }

                //Draw the shorter leg of the trip
                if(i < drawShorterSection.size()){

                    if(i < drawShorterSection.size()-1) {
                        //Drawing outline
                        mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(drawShorterSection.get(i).latitude, drawShorterSection.get(i).longitude),
                                        new LatLng(drawShorterSection.get(i + 1).latitude, drawShorterSection.get(i + 1).longitude))
                                .width(LINE_WIDTH + OUTLINE_WIDTH)
                                .color(getColor(OUTLINE_COLOR)));

                        //Drawing colored line
                        mMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(drawShorterSection.get(i).latitude, drawShorterSection.get(i).longitude),
                                        new LatLng(drawShorterSection.get(i + 1).latitude, drawShorterSection.get(i + 1).longitude))
                                .width(LINE_WIDTH)
                                .color(getColor(shorterLine)));
                    }
                }
            }

            //Determine the intersection station (needed for the textual description)
            int intersection = 0;
            if(path.startIndex == 0)
                intersection = path.firstLeg.size()-1;

            //Add map markers for the starting, intersection, and ending stations
            mMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(path.startIndex).latitude,
                    path.firstLeg.get(path.startIndex).longitude)).title("Start: " + path.firstLeg.get(path.startIndex).name)));
            mMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(path.firstLeg.get(intersection).latitude,
                    path.firstLeg.get(intersection).longitude)).title("Transfer: " + path.firstLeg.get(intersection).name)));
            mMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(path.secondLeg.get(path.endIndex).latitude,
                    path.secondLeg.get(path.endIndex).longitude)).title("End: " + path.secondLeg.get(path.endIndex).name)));



            //Set the textual directions
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

        tvDirections.setText(desc);

        //Set the background colors for both of the ImageViews
        //Hide the second colored ImageView if there's only one leg to the trip
        if(path.sameLine){
            ivFirstLegColor.setBackgroundColor(getColor(path.startLine));
            ivSecondLegColor.setVisibility(View.INVISIBLE);
        }else{
            ivSecondLegColor.setVisibility(View.VISIBLE);
            ivFirstLegColor.setBackgroundColor(getColor(path.startLine));
            ivSecondLegColor.setBackgroundColor(getColor(path.endLine));
        }

        //Move & zoom the camera to fit the entire metro path on screen
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        //Camera is set to fit all the markers on the screen
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING);
        mMap.animateCamera(cu);
    }

    /**
     * Returns the spelled-out color of a line's color code
     * @param code
     * @return
     */
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


    /**
     * Returns a COLOR value for a specific line code
     * @param line
     * @return
     */
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
     * ---- Auto-Generated Method ----
     *
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.
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
               Log.d(MainActivity.TAG, "Error setting up Map");
            }
        }
    }

    /**
     * Receives messages from the Planning Module for network timeouts, progress bar updates,
     * success / failure, etc.
     */
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
                    mIsPlanning = false;
                    mPaths = (ArrayList<MetroPath>)message.obj;
                    onPlanningModuleDone();
                    mDialog.cancel();
                    break;
                case HandlerCodes.PLANNING_MODULE_ERR:
                    mDialog.cancel();
                    mIsPlanning = false;
                    Toast.makeText(MapsActivity.this, (String)message.obj, Toast.LENGTH_LONG).show();
                    if(!mRetryShowing)
                        showRetryDialog((String)message.obj);
                    break;
                case HandlerCodes.TIMEOUT:
                    mDialog.cancel();
                    mIsPlanning = false;
                    Toast.makeText(MapsActivity.this, "Network timeout: please make sure you have networking services enabled.", Toast.LENGTH_LONG).show();
                    if(!mRetryShowing)
                        showRetryDialog("Network timeout: please make sure you have networking services enabled.");
                    break;
            }
        }
    };

    /**
     * Allows the user to restart the Planning Module if there was a network problem, for example
     * @param errMsg
     */
    private void showRetryDialog(String errMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error Finding Metro Path");
        builder.setMessage(errMsg);
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mRetryShowing = false;
                mIsPlanning = true;
                mPlanningModule = new PlanningModule(mStartingAddr, mEndingAddr, mHandler);
                showProgessDialog();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(HandlerCodes.START_PLANNING_MODULE), ONE_SECOND);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mRetryShowing = false;
                dialog.cancel();
                MapsActivity.this.finish();
            }
        });
        mRetryShowing = true;
        builder.setCancelable(false);
        builder.create().show();
    }
}
