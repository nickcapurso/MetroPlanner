package com.example.nickcapurso.mapsapplication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Metro API requests to get information about the metro stations, lines, etc. to
 * build up metro paths between the starting address and the ending address
 */
//TODO thoughts - for the future, maybe compute all the pairwise shortest paths at app first run. Recompute on incidents? How to know which shortest paths need to be
//TODO recomputed? Need to recompute the whole map, but virtually this would happen at every app start up (assuming one incident per day or something)
public class PlanningModule{
    //Constants for each of the states
    private static final byte STATE_GET_ENTRANCES = 0;
    private static final byte STATE_GET_STATION_INFO = 1;
    private static final byte STATE_GET_ALT_LINES = 2;
    private static final byte STATE_GET_STATION_LIST = 3;
    private static final byte STATE_FINISHED = 4;
    private static final byte STATE_ERR = 6;

    private static final int ONE_SECOND = 1000;

    /**
     * Use a delay for accessing the metro API so that the number of requests per second does
     * not exceed the allowed quota (non-commercial license)
     */
    private static final int API_DELAY = ONE_SECOND / 2;

    /**
     * Look for station entrances within a mile of the inputted addresses
     */
    private static final int MILE_IN_METERS = 1610;

    /**
     * Line codes for the metro lines (should be made generic in the future - by fetching
     * the actual line codes from the Metro API)
     */
    private static final String[] lines = { "RD", "BL", "OR", "SV", "YL", "GR"};

    /**
     * For each line, hold the stations that comprise the line (in the future, maybe
     * store this in a file / database)
     */
    private Map<String, ArrayList<StationInfo>> mAllLines;

    /**
     * The current state (see state constants at the top)
     */
    private byte mState;

    /**
     * Either the first query or the second query in a particular state
     */
    private byte mCurrQueryNum;

    /**
     * The starting address (as inputted by the user in the main activity)
     */
    private final AddressInfo mStartingAddr;

    /**
     * The ending address (as inputted by the user in the main activity)
     */
    private final AddressInfo mEndingAddr;

    /**
     * StationInfo object for the closest station to the starting address
     */
    private StationInfo mStartingStation;

    /**
     * StationInfo object for the closest station to the ending address
     */
    private StationInfo mEndingStation;

    /**
     * List of metro paths between the starting station and the ending station
     */
    private ArrayList<MetroPath> mPaths;

    /**
     * Reference to the MapsActivity's handler to send results to
     */
    private final Handler mClientHandler;

    /**
     * Error message set when the result of a network request can't be parsed
     */
    private String mErrMsg;

    /**
     * Sets variables and initializes arrays
     * @param startingAddr The user-inputted starting address
     * @param endingAddr The user-inputted ending address
     * @param client Reference to the MapsActivity's handler
     */
    public PlanningModule(AddressInfo startingAddr, AddressInfo endingAddr, Handler client){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
        mPaths = new ArrayList<MetroPath>();
        mAllLines = new HashMap<String, ArrayList<StationInfo>>();
    }

    /**
     * Called to begin execute of the Planning Module. The first request is to find
     * the metro station that is closest to the starting address
     */
    public void start(){
        mState = STATE_GET_ENTRANCES;
        mCurrQueryNum = 0;
        Log.d(MainActivity.TAG, "Fetching first station entrance");
        new JSONFetcher(mHandler).execute(API_URLS.STATION_ENTRANCES, "api_key", API_URLS.WMATA_API_KEY,
                "Lat", ""+mStartingAddr.latitude, "Lon", ""+mStartingAddr.longitude, "Radius", ""+ MILE_IN_METERS);
    }

    /**
     * Called upon the return of a network request including data in JSON form. Different actions
     * are performed depending on the current state (mState):
     *      STATE_GET_ENTRANCES:
     *          - First query - parse the metro station closest to the starting address
     *          - Second query - parse the metro station closest to the ending address
     *      STATE_GET_STATION_INFO:
     *          - First query - set the mStartingStation object with information about this station
     *          (its name, code, lines, etc.)
     *          - Second query - set the mEndingStation object with information about this station
     *          (its name, code, lines, etc.)
     *      STATE_GET_ALT_LINES:
     *          - First query - Parse any alternate lines that pass through the starting station
     *          (that aren't included by its station code, but instead its altCodes)
     *          - First query - Parse any alternate lines that pass through the ending station
     *          (that aren't included by its station code, but instead its altCodes)
     *      STATE_GET_STATION_LIST:
     *          - Get the list of stations for each line, when all lines are fetched, the
     *          metro path between the startingStation and the endingStation can be constructed
     *
     * If any errors occur while parsing the JSON results, the module goes into an ERR state, which
     * causes an error message to be sent back to the MapsActivity.
     *
     * Before returning, the method sends a delayed message to the module's handler - which, upon
     * reception, starts the next network query depending on the current state. This delay ensures
     * that the app does not go over the maximum number of queries allowed per second.
     *
     * @param jsonResult The result of a network query using the Metro API - returned in a JSON object
     */
    private void parseFetchResults(String jsonResult){
        Message msg = mHandler.obtainMessage(HandlerCodes.FETCH_DELAY_DONE);
        JSONObject topObject;

        //Error checking if the returned results are null or empty
        if(jsonResult == null || jsonResult.equals("")){
            String errMsg = "Error: no response from WMATA - make sure you have networking services enabled.";
            mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, errMsg));
            return;
        }

        //If the results are not empty, check to see if the statusCode is set (indicating an error)
        try {
            topObject = new JSONObject(jsonResult);
            if(!topObject.isNull("statusCode")){
                String errMsg = "Error (" + topObject.getInt("statusCode") + "): " + topObject.getString("message");
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, errMsg));
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch(mState){
            //Parse out the closest metro stations to the starting / ending address
            case STATE_GET_ENTRANCES:
                updateProgress(1);
                if(mCurrQueryNum == 0) {
                    mStartingStation = parseStationEntrances(jsonResult);
                }else {
                    mEndingStation = parseStationEntrances(jsonResult);
                }

                //Set error message if no stations in range
                if(mState == STATE_ERR)
                    mErrMsg = "No metro station entrances within a mile of " + ((mCurrQueryNum == 0)?  "starting address" : "ending address");
                break;

            //Parse out the information about the starting / ending metro stations
            case STATE_GET_STATION_INFO:
                updateProgress(1);
                if(mCurrQueryNum == 0){
                    mStartingStation = parseStationInfo(jsonResult);
                }else{
                    mEndingStation = parseStationInfo(jsonResult);
                }

                //Set error message if parsing fails
                if(mState == STATE_ERR)
                    mErrMsg = "Error receiving information for metro stations near " + ((mCurrQueryNum == 0)?  "starting address" : "ending address");
                break;

            //Parse any alternative lines (indicated by the presence of altCode fields found
            //during the GET_STATION_INFO state)
            case STATE_GET_ALT_LINES:
                if(mCurrQueryNum == 0) {
                    //updateProgress(1);
                    mStartingStation.lines = parseAltLines(mStartingStation.lines, jsonResult);
                }else{
                    //updateProgress(1);
                    mEndingStation.lines = parseAltLines(mEndingStation.lines, jsonResult);
                }

                //Set error message if parsing fails
                if(mState == STATE_ERR)
                    mErrMsg = "Error receiving metro lines information";
                break;

            //Get the list of stations that lie on a particular line, then when all are obtained,
            //calculate a path from the starting station to the ending station
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum < lines.length-1){
                    if(!mAllLines.containsKey(lines[mCurrQueryNum]))
                        mAllLines.put(lines[mCurrQueryNum], parseStationList(jsonResult));

                }else{
                    //Parse final station list
                    if(!mAllLines.containsKey(lines[mCurrQueryNum]))
                        mAllLines.put(lines[mCurrQueryNum], parseStationList(jsonResult));

                    //Set error message if parsing fails
                    if(mState == STATE_ERR) {
                        mErrMsg = "Error receiving metro lines information";
                        break;
                    }

                    //Determine what lines are shared by the starting and ending stations
                    ArrayList<String> commonLines = getCommonLines(mStartingStation, mEndingStation);

                    //If there are shared lines, then these simply form the path between the starting
                    //and ending stations
                    if(commonLines.size() != 0){
                        updateProgress(2);
                        mState = STATE_FINISHED;

                        //Add a path for each shared line
                        for(String s : commonLines)
                            mPaths.add(getPath(mAllLines.get(s), s, mStartingStation, null, s, mEndingStation));
                        break;
                    }

                    //TODO station may have perpendicular lines, where the first line may not be shared with the others
                    //In the case of the D.C. Metro, all lines intersect every other line at some point, so determining
                    //the path based on an arbitrarily chosen line (running through either the starting/ending
                    // station) is reasonable.
                    MetroPath firstPath = getPath(mAllLines.get(mStartingStation.lines.get(0)), mStartingStation.lines.get(0), mStartingStation,
                            mAllLines.get(mEndingStation.lines.get(0)), mEndingStation.lines.get(0), mEndingStation);

                    //Determine the intersection station (either the first or last station in the arraylist)
                    StationInfo intersectionStation1, intersectionStation2;
                    if(firstPath.startIndex == 0)
                        intersectionStation1 = firstPath.firstLeg.get(firstPath.firstLeg.size()-1);
                    else
                       intersectionStation1 = firstPath.firstLeg.get(0);

                    //Same for the second leg
                    if(firstPath.endIndex == 0)
                        intersectionStation2 = firstPath.secondLeg.get(firstPath.secondLeg.size()-1);
                    else
                        intersectionStation2 = firstPath.secondLeg.get(0);

                    Log.d(MainActivity.TAG, "Intersection station (2) = " + intersectionStation1.name);

                    //Add the first determined path to mPaths (list of paths from source/destination)
                    mPaths.add(firstPath);

                    //Also need to add a path for all shared lines between:
                    //  starting station -> intersection station
                    //  intersection station -> ending station
                    for(String i : getCommonLines(mStartingStation, intersectionStation1)) {
                        Log.d(MainActivity.TAG, "First leg common line: " + i);
                        for (String j : getCommonLines(intersectionStation2, mEndingStation)) {
                            if(i.equals(firstPath.startLine) && j.equals(firstPath.endLine))
                                continue;
                            Log.d(MainActivity.TAG, "Second leg common line: " + j);

                            //TODO Don't like this, too much redundant work
                            mPaths.add(getPath(
                                    mAllLines.get(i), i, mStartingStation,
                                    mAllLines.get(j), j, mEndingStation
                            ));
                        }
                    }

                    //TODO next, consider "new lines" while iterating through the station list and going "up" or "down" the line (minimize # stations / time)
                    updateProgress(2);
                    mState = STATE_FINISHED;
                }
                break;
        }

        //Send handler messages to:
        //  MapsActivity (on parsing errors or successful completion)
        //  Self (to create the API-call delay)
        if(mState == STATE_ERR)
            mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, mErrMsg));
        else if(mState != STATE_FINISHED)
            mHandler.sendMessageDelayed(msg, API_DELAY);
        else
            mClientHandler.sendMessageDelayed(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_DONE, mPaths), 2*API_DELAY);
    }

    /**
     * Determines a path between a starting station and an ending station (given their lines). For
     * stations on the same line, the path is simply the set of shared lines. Otherwise, need to determine
     * the intersection between the lines that the two stations lie on.
     * @param line1 The set of stations that comprises a line that the starting station lies on (ex. "Foggy Bottom", "Farragut West", etc.)
     * @param line1Color The line's color (ex. "SV" for silver)
     * @param startStation The starting station
     * @param line2 The set of stations that comprises a line that the ending station lies on
     * @param line2Color The line's color
     * @param endStation The ending station
     * @return
     */
    private MetroPath getPath(ArrayList<StationInfo> line1, String line1Color, StationInfo startStation, ArrayList<StationInfo> line2, String line2Color, StationInfo endStation){
        int startIndex, intersectionIndex1, intersectionIndex2, endIndex;
        StationInfo intersectionStation;

        MetroPath path = new MetroPath();

        //If the two line colors are equal, the two stations lie on the same line
        if(line1Color.equals(line2Color)){
            //Set up fields in the MetroPath object
            path.sameLine = true;
            startIndex = line1.indexOf(startStation);
            endIndex = line1.indexOf(endStation);

            //Truncate the station list s.t. the first and last elements are the starting/ending stations (in either order)
            path.firstLeg = getTripLeg(line1, startIndex, endIndex);
            path.startLine = line1Color;

            //Determine whether the starting/ending station is the first or last element in the truncated list
            if(path.firstLeg.get(0).equals(mStartingStation)){
                path.startIndex = 0;
                path.endIndex = path.firstLeg.size()-1;
            }else{
                path.startIndex = path.firstLeg.size()-1;
                path.endIndex = 0;
            }

            //Set the "lineTowards" field (i.e. "Take the orange line *towards* Vienna)
            if(startIndex <= endIndex)
                path.lineTowards.put(line1Color, line1.get(line1.size()-1).name);
            else
                path.lineTowards.put(line1Color, line1.get(0).name);

        //Else, the two stations don't lie on the same lines
        }else{
            //Set up fields in the MetroPath object
            startIndex = line1.indexOf(startStation);
            endIndex = line2.indexOf(endStation);

            //Determine the intersection between the two lines
            intersectionIndex1 = getLinesIntersection(line1, line2, startIndex);
            intersectionStation = line1.get(intersectionIndex1);
            intersectionIndex2 = line2.indexOf(intersectionStation);

            Log.d(MainActivity.TAG, "Intersection at: " + intersectionStation.name);

            //Set the "lineTowards" field for the first leg of the trip(i.e. "Take the orange line *towards* Vienna)
            if(startIndex <= intersectionIndex1)
                path.lineTowards.put(line1Color, line1.get(line1.size()-1).name);
            else
                path.lineTowards.put(line1Color, line1.get(0).name);

            //Set the "lineTowards" field for the second leg of the trip(i.e. "Take the orange line *towards* Vienna)
            if(intersectionIndex2 <= endIndex)
                path.lineTowards.put(line2Color, line2.get(line2.size()-1).name);
            else
                path.lineTowards.put(line2Color, line2.get(0).name);

            //Set more fields in the MetroPath object
            path.startLine = line1Color;
            path.sameLine = false;
            path.endLine = line2Color;

            //Truncate the station lists so that the first/last elements of the two legs are
            //one of: starting station, intersection station, or the ending station
            path.firstLeg = getTripLeg(line1, startIndex, intersectionIndex1);
            path.secondLeg = getTripLeg(line2, intersectionIndex2, endIndex);

            //Determine whether the starting station is the first or last element in the truncated list
            if(path.firstLeg.get(0).equals(mStartingStation))
                path.startIndex = 0;
            else
                path.startIndex = path.firstLeg.size()-1;

            //Determine whether ending station is the first or last element in the truncated list
            if(path.secondLeg.get(0).equals(mEndingStation))
                path.endIndex = 0;
            else
                path.endIndex = path.secondLeg.size()-1;

        }


        return path;
    }

    /**
     * Called after a small delay so as to not overrun the maximum allowed Metro API queries per second.
     * Starts the next network request depending on the current state (refer to the documentation for
     * parseFetchResuls(...)
     */
    private void continueFetches(){
        switch(mState){
            case STATE_GET_ENTRANCES:
                //First query complete, time to get the station entrance for the second address
                if(mCurrQueryNum == 0) {
                    mCurrQueryNum++;
                    Log.d(MainActivity.TAG, "Fetching second station entrance");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_ENTRANCES, "api_key", API_URLS.WMATA_API_KEY,
                            "Lat", ""+mEndingAddr.latitude, "Lon", ""+mEndingAddr.longitude, "Radius", ""+ MILE_IN_METERS);

                //Second query complete, time to get the station info (and therefore the lines) for the starting station
                }else {
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_INFO;
                    //Run JSONfetcher (get lines)
                    Log.d(MainActivity.TAG, "Fetching first station info");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mStartingStation.code);
                }
                break;
            case STATE_GET_STATION_INFO:

                //First query complete, time to get the station info (and therefore the lines) for the ending station
                if(mCurrQueryNum == 0){
                    mCurrQueryNum ++;
                    //Run JSONfetcher (second get startLine)
                    Log.d(MainActivity.TAG, "Fetching second station info");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mEndingStation.code);

                //Fetch any lines that corresponds to a station's altCode
                }else{

                    //Both the starting and ending stations may have altCode set (for additional lines)
                    if(!mStartingStation.altCode1.equals("")){
                        mCurrQueryNum = 0;
                        mState = STATE_GET_ALT_LINES;
                        Log.d(MainActivity.TAG, "Fetching first station alt lines");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                                "StationCode", mStartingStation.altCode1);
                    }else if(!mEndingStation.altCode1.equals("")){
                        mState = STATE_GET_ALT_LINES;
                        Log.d(MainActivity.TAG, "Fetching second station alt lines");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                                "StationCode", mEndingStation.altCode1);
                    }else{
                        mCurrQueryNum = 0;
                        mState = STATE_GET_STATION_LIST;

                        Log.d(MainActivity.TAG, "Fetching first station list");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                                "LineCode", lines[0]);

                    }
                }
                break;
            case STATE_GET_ALT_LINES:
                if(mCurrQueryNum == 0){

                    //If the starting station had its alternate lines fetched, but we still may
                    //need to fetch alternate lines for the ending station
                    if(!mEndingStation.altCode1.equals("")){
                        mCurrQueryNum ++;
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                                "StationCode", mEndingStation.altCode1);

                    //Move on to fetching station lists for each line
                    }else{
                        mCurrQueryNum = 0;
                        mState = STATE_GET_STATION_LIST;

                        Log.d(MainActivity.TAG, "Fetching first station list");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                                "LineCode", lines[0]);
                    }

                //Move on to fetching station lists for each line
                }else {
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_LIST;

                    Log.d(MainActivity.TAG, "Fetching first station list");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", lines[0]);
                }
                break;
            case STATE_GET_STATION_LIST:
                //Fetch the station lists for each line
                if(mCurrQueryNum < lines.length-1){
                    mCurrQueryNum ++;
                    Log.d(MainActivity.TAG, "Fetching list for line: " + lines[mCurrQueryNum]);
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY, "LineCode", lines[mCurrQueryNum]);
                }
                break;
        }
    }

    /**
     * Determine the intersection between two lines. The index of the intersection station
     * within the first line's list is returned.
     * @param line1 List of stations that comprise the first line
     * @param line2 List of stations that comprise the second line
     * @param startIndex Index of the starting station within the first line's list
     * @return
     */
    private int getLinesIntersection(ArrayList<StationInfo> line1, ArrayList<StationInfo> line2, int startIndex){
        Log.d(MainActivity.TAG, "Finding intersection station...");
        for(int i = startIndex; i < line1.size(); i++)
            if(line2.contains(line1.get(i)))
                return i;

        //Iterate in the reverse section if an intersection station wasn't found
        Log.d(MainActivity.TAG, "Finding intersection station (iterating in reverse)...");
        for(int i = startIndex; i > 0; i--)
            if(line2.contains(line1.get(i)))
                return i;

        return -1;
    }

    /**
     * Takes in a full list of stations and returns a subset of the list where the two passed indexes
     * are the first and last stations.
     * @param line
     * @param index1
     * @param index2
     * @return
     */
    private ArrayList<StationInfo> getTripLeg(ArrayList<StationInfo> line, int index1, int index2){
        if(index1 <= index2){
            return new ArrayList<StationInfo>(line.subList(index1,index2+1));
        }else{
            return new ArrayList<StationInfo>(line.subList(index2,index1+1));
        }
    }

    /**
     * Determines the common lines between two StationInfo objects.
     * @param startingStation
     * @param endingStation
     * @return List of the shared lines
     */
    private ArrayList<String> getCommonLines(StationInfo startingStation, StationInfo endingStation){
        ArrayList<String> commonLines = new ArrayList<String>();

        for(String line : startingStation.lines)
            if(endingStation.lines.contains(line)) {
                commonLines.add(line);
                Log.d(MainActivity.TAG, "Common line: " + line);
            }

        return commonLines;
    }

    /**
     * Parses station entrance info out of a JSON object returned by a Metro API query
     * @param jsonResult
     * @return A StationInfo object created from parsing
     */
    private StationInfo parseStationEntrances(String jsonResult){
        StationInfo temp = new StationInfo();
        try {
            JSONObject topObject = new JSONObject(jsonResult);
            JSONArray entrances = topObject.getJSONArray("Entrances");
            JSONObject firstEntrance = entrances.getJSONObject(0);

            temp.name = firstEntrance.getString("Name");
            temp.latitude = firstEntrance.getDouble("Lat");
            temp.longitude = firstEntrance.getDouble("Lon");
            temp.code = firstEntrance.getString("StationCode1");
        } catch (JSONException e) {
            mState = STATE_ERR;
            e.printStackTrace();
        }
        Log.d(MainActivity.TAG, "Found station: " + temp.name + ", lat: " + temp.latitude + ", lng: " + temp.longitude + ", code: " + temp.code);
        return temp;
    }

    /**
     * Parses station info out of a JSON object returned by a Metro API query
     * @param jsonResult
     * @return A StationInfo object created from parsing
     */
    private StationInfo parseStationInfo(String jsonResult){
        StationInfo temp = new StationInfo();
        try {
            JSONObject topObject = new JSONObject(jsonResult);
            temp = new StationInfo(topObject.getString("Name"), topObject.getDouble("Lat"), topObject.getDouble("Lon"), topObject.getString("Code"),
                    topObject.getString("StationTogether1"), topObject.getString("StationTogether2"));

            if(!topObject.isNull("LineCode1"))
                temp.lines.add(topObject.getString("LineCode1"));

            if(!topObject.isNull("LineCode2"))
                temp.lines.add(topObject.getString("LineCode2"));

            if(!topObject.isNull("LineCode3"))
                temp.lines.add(topObject.getString("LineCode3"));

            String debug = "{";
            for(String s : temp.lines){
                debug += s + ", ";
            }
            Log.d(MainActivity.TAG, "Lines now includes: " + debug + "}");

        } catch (JSONException e) {
            mState = STATE_ERR;
            e.printStackTrace();
        }

        return temp;
    }

    /**
     * Parses a list of (alternate) lines info out of a JSON object returned by a Metro API query
     * @param jsonResult
     * @return The list of lines
     */
    private ArrayList<String> parseAltLines(ArrayList<String> currLines, String jsonResult){
        ArrayList<String> lines = currLines;
        try {
            JSONObject topObject = new JSONObject(jsonResult);

            if(!topObject.isNull("LineCode1"))
                lines.add(topObject.getString("LineCode1"));

            if(!topObject.isNull("LineCode2"))
                lines.add(topObject.getString("LineCode2"));

            if(!topObject.isNull("LineCode3"))
                lines.add(topObject.getString("LineCode3"));

            String debug = "{";
            for(String s : lines){
                debug += s + ", ";
            }
            Log.d(MainActivity.TAG, "Alt lines now includes: " + debug + "}");

        } catch (JSONException e) {
            mState = STATE_ERR;
            e.printStackTrace();
        }
        return lines;
    }

    /**
     * Parses a list of stations (i.e. a list of StationInfo objects) out of a JSON object returned by a Metro API query
     * @param jsonResult
     * @return The list of stations (array of StationInfo objects)
     */
    private ArrayList<StationInfo> parseStationList(String jsonResult){
        ArrayList<StationInfo> temp = new ArrayList<StationInfo>();

        try {
            JSONObject topObject = new JSONObject(jsonResult);
            JSONArray stationsList = topObject.getJSONArray("Stations");

            for(int i = 0; i < stationsList.length(); i++){
                JSONObject station = stationsList.getJSONObject(i);
                temp.add(new StationInfo(station.getString("Name"), station.getDouble("Lat"), station.getDouble("Lon"), station.getString("Code"),
                        station.getString("StationTogether1"), station.getString("StationTogether2")));

                if(!station.isNull("LineCode1"))
                    temp.get(i).lines.add(station.getString(("LineCode1")));

                if(!station.isNull("LineCode2"))
                    temp.get(i).lines.add(station.getString(("LineCode2")));

                if(!station.isNull("LineCode3"))
                    temp.get(i).lines.add(station.getString(("LineCode3")));
            }
        } catch (JSONException e) {
            mState = STATE_ERR;
            e.printStackTrace();
        }

        for(StationInfo info : temp)
            Log.d(MainActivity.TAG, "Added, name: " + info.name + ", lat: " + info.latitude + ", lon: " + info.longitude + ", code: " + info.code
            + ", altCode1: " + info.altCode1 + ", altCode2: " + info.altCode2);
        return temp;
    }

    /**
     * Sends a message to the MapsActivity to update its progress bar
     * @param amount
     */
    private void updateProgress(int amount){
        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, amount));
    }

    /**
     * Receives messages from the JSONFetcher, NetworkTimeout, and from itself (the API delay)
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            Log.d(MainActivity.TAG, "PlanningModule, message received ("+message.what+")");
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    parseFetchResults((String) message.obj);
                    break;
                case HandlerCodes.JSON_FETCH_ERR:
                    mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, "Error receiving data from WMATA"));
                    break;
                case HandlerCodes.FETCH_DELAY_DONE:
                    continueFetches();
                    break;
                case HandlerCodes.TIMEOUT:
                    mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, "Network error: please make sure you have networking services enabled."));
                    break;
            }
        }
    };
}
