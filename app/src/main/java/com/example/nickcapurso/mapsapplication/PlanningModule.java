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
 * Created by nickcapurso on 3/18/15.
 */
//TODO thoughts - for the future, maybe compute all the pairwise shortest paths at app first run. Recompute on incidents? How to know which shortest paths need to be
//TODO recomputed? Need to recompute the whole map, but virtually this would happen at every app start up (assuming one incident per day or something)

//TODO need to fill out all the catch blocks of the try-catch statements - should send PLANNING_MODULE_ERR message
public class PlanningModule{
    private static final byte STATE_GET_ENTRANCES = 0;
    private static final byte STATE_GET_STATION_INFO = 1;
    private static final byte STATE_GET_ALT_LINES = 2;
    private static final byte STATE_GET_STATION_LIST = 3;
    private static final byte STATE_FINISHED = 4;
    private static final int HALF_MILE_IN_METERS = 805;
    private static final int ONE_SECOND = 1000;
    private static final int API_DELAY = ONE_SECOND / 2;

    private static final String[] lines = { "RD", "BL", "OR", "SV", "YL", "GR"};
    private Map<String, ArrayList<StationInfo>> mAllLines;

    private byte mState, mCurrQueryNum;
    private final AddressInfo mStartingAddr, mEndingAddr;
    private StationInfo mStartingStation, mEndingStation;
    //private ArrayList<ArrayList<StationInfo>> mStartingLines, mEndingLines;
    private ArrayList<MetroPath> mPaths;
    private final Handler mClientHandler;

    public PlanningModule(AddressInfo startingAddr, AddressInfo endingAddr, Handler client){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
        mPaths = new ArrayList<MetroPath>();

        //mStartingLines = new ArrayList<ArrayList<StationInfo>>();
        //mEndingLines = new ArrayList<ArrayList<StationInfo>>();
        mAllLines = new HashMap<String, ArrayList<StationInfo>>();
    }

    public void start(){
        mState = STATE_GET_ENTRANCES;
        mCurrQueryNum = 0;
        Log.d(MainActivity.TAG, "Fetching first station entrance");
        new JSONFetcher(mHandler).execute(API_URLS.STATION_ENTRANCES, "api_key", API_URLS.WMATA_API_KEY,
                "Lat", ""+mStartingAddr.latitude, "Lon", ""+mStartingAddr.longitude, "Radius", ""+HALF_MILE_IN_METERS);
    }

    private void parseFetchResults(String jsonResult){
        Message msg = mHandler.obtainMessage(HandlerCodes.FETCH_DELAY_DONE);
        JSONObject topObject;

        if(jsonResult == null || jsonResult.equals("")){
            String errMsg = "Error: no response from WMATA - make sure you have networking services enabled.";
            mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_ERR, errMsg));
            return;
        }

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
            //TODO need to stop if no entrances found
            case STATE_GET_ENTRANCES:
                updateProgress(1);
                if(mCurrQueryNum == 0) {
                    mStartingStation = parseStationEntrances(jsonResult);
                }else {
                    mEndingStation = parseStationEntrances(jsonResult);
                }
                break;
            case STATE_GET_STATION_INFO:
                updateProgress(1);
                if(mCurrQueryNum == 0){
                    mStartingStation = parseStationInfo(jsonResult);
                }else{
                    mEndingStation = parseStationInfo(jsonResult);
                }
                break;
            case STATE_GET_ALT_LINES:
                if(mCurrQueryNum == 0) {
                    //updateProgress(1);
                    mStartingStation.lines = parseAltLines(mStartingStation.lines, jsonResult);
                }else{
                    //updateProgress(1);
                    mEndingStation.lines = parseAltLines(mEndingStation.lines, jsonResult);
                }
                break;
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum < lines.length-1){
                    if(!mAllLines.containsKey(lines[mCurrQueryNum]))
                        mAllLines.put(lines[mCurrQueryNum], parseStationList(jsonResult));

                }else{
                    //Parse final station list
                    if(!mAllLines.containsKey(lines[mCurrQueryNum]))
                        mAllLines.put(lines[mCurrQueryNum], parseStationList(jsonResult));


                    ArrayList<String> commonLines = getCommonLines(mStartingStation, mEndingStation);

                    if(commonLines.size() != 0){
                        updateProgress(2);
                        mState = STATE_FINISHED;

                        for(String s : commonLines)
                            mPaths.add(getPath(mAllLines.get(s), s, mStartingStation, null, s, mEndingStation));
                        break;
                    }



                    //TODO station may have perpendicular lines, where the first line may not be shared with the others
                    MetroPath firstPath = getPath(mAllLines.get(mStartingStation.lines.get(0)), mStartingStation.lines.get(0), mStartingStation,
                            mAllLines.get(mEndingStation.lines.get(0)), mEndingStation.lines.get(0), mEndingStation);

                    StationInfo intersectionStation1, intersectionStation2;
                    if(firstPath.startIndex == 0)
                        intersectionStation1 = firstPath.firstLeg.get(firstPath.firstLeg.size()-1);
                    else
                       intersectionStation1 = firstPath.firstLeg.get(0);

                    //TODO: thought - maybe merge both first leg and second leg so that I don't need to do two intersection stations
                    //TODO: problem - alt line codes (ex. L'Enfant plaza has two platforms, two codes)
                    if(firstPath.endIndex == 0)
                        intersectionStation2 = firstPath.secondLeg.get(firstPath.secondLeg.size()-1);
                    else
                        intersectionStation2 = firstPath.secondLeg.get(0);

                    Log.d(MainActivity.TAG, "Intersection station (2) = " + intersectionStation1.name);
                    mPaths.add(firstPath);

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

                    //addSharedLines(firstPath);



                    //TODO next, consider "new lines" while iterating through the station list and going "up" or "down" the line (station count)
                    updateProgress(2);
                    mState = STATE_FINISHED;
                }
                break;
        }
        if(mState != STATE_FINISHED)
            mHandler.sendMessageDelayed(msg, API_DELAY);
        else
            mClientHandler.sendMessageDelayed(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_DONE, mPaths), 2*API_DELAY);
    }

    private MetroPath getPath(ArrayList<StationInfo> line1, String line1Color, StationInfo startStation, ArrayList<StationInfo> line2, String line2Color, StationInfo endStation){
        int startIndex, intersectionIndex1, intersectionIndex2, endIndex;
        StationInfo intersectionStation;

        MetroPath path = new MetroPath();

        if(line1Color.equals(line2Color)){
            path.sameLine = true;
            startIndex = line1.indexOf(startStation);
            endIndex = line1.indexOf(endStation);
            path.firstLeg = getTripLeg(line1, startIndex, endIndex);
            path.startLine = line1Color;

            if(path.firstLeg.get(0).equals(mStartingStation)){
                path.startIndex = 0;
                path.endIndex = path.firstLeg.size()-1;
            }else{
                path.startIndex = path.firstLeg.size()-1;
                path.endIndex = 0;
            }

            if(startIndex <= endIndex)
                path.lineTowards.put(line1Color, line1.get(line1.size()-1).name);
            else
                path.lineTowards.put(line1Color, line1.get(0).name);

        }else{
            startIndex = line1.indexOf(startStation);
            endIndex = line2.indexOf(endStation);
            intersectionIndex1 = getLinesIntersection(line1, line2, startIndex);
            intersectionStation = line1.get(intersectionIndex1);
            intersectionIndex2 = line2.indexOf(intersectionStation);

            Log.d(MainActivity.TAG, "Intersection at: " + intersectionStation.name);

            if(startIndex <= intersectionIndex1)
                path.lineTowards.put(line1Color, line1.get(line1.size()-1).name);
            else
                path.lineTowards.put(line1Color, line1.get(0).name);

            if(intersectionIndex2 <= endIndex)
                path.lineTowards.put(line2Color, line2.get(line2.size()-1).name);
            else
                path.lineTowards.put(line2Color, line2.get(0).name);

            path.startLine = line1Color;
            path.sameLine = false;
            path.endLine = line2Color;
            path.firstLeg = getTripLeg(line1, startIndex, intersectionIndex1);
            path.secondLeg = getTripLeg(line2, intersectionIndex2, endIndex);

            if(path.firstLeg.get(0).equals(mStartingStation))
                path.startIndex = 0;
            else
                path.startIndex = path.firstLeg.size()-1;


            if(path.secondLeg.get(0).equals(mEndingStation))
                path.endIndex = 0;
            else
                path.endIndex = path.secondLeg.size()-1;

        }


        return path;
    }



    private void addSharedLines(MetroPath path){
        StationInfo intersection;
        String currLineColor;
        ArrayList<StationInfo> currLine;
        int startIndex, intersectionIndex, endIndex;

        if(path.firstLeg.indexOf(mStartingStation) == 0)
            intersection = path.firstLeg.get(path.firstLeg.size()-1);
        else
            intersection = path.firstLeg.get(0);


        Log.d(MainActivity.TAG, "mSS location: " + path.firstLeg.indexOf(mStartingStation));
        if(mStartingStation.lines.size() > 1) {
            for (int i = 1; i < mStartingStation.lines.size(); i++) {
                currLineColor = mStartingStation.lines.get(i);
                currLine = mAllLines.get(currLineColor);
                Log.d(MainActivity.TAG, "Curr line " + currLineColor);

                if (currLine.contains(intersection)) {
                    path.firstLegSharedLines.add(currLineColor);
                    Log.d(MainActivity.TAG, "First leg shared with: " + currLine);

                    startIndex = currLine.indexOf(mStartingStation);
                    intersectionIndex = currLine.indexOf(intersection);

                    if (startIndex <= intersectionIndex)
                        path.lineTowards.put(currLineColor, currLine.get(currLine.size() - 1).name);
                    else
                        path.lineTowards.put(currLineColor, currLine.get(0).name);
                }
            }
        }

        if(mEndingStation.lines.size() <= 1)
            return;

        for(int i = 1; i < mEndingStation.lines.size(); i++){
            currLineColor = mStartingStation.lines.get(i);
            currLine =  mAllLines.get(currLineColor);

            if(currLine.contains(intersection)){
                path.secondLegSharedLines.add(currLineColor);
                Log.d(MainActivity.TAG, "Second leg shared with: " + currLineColor);

                endIndex = currLine.indexOf(mEndingStation);
                intersectionIndex = currLine.indexOf(intersection);

                if(intersectionIndex <= endIndex)
                    path.lineTowards.put(currLineColor, currLine.get(currLine.size()-1).name);
                else
                    path.lineTowards.put(currLineColor, currLine.get(0).name);
            }
        }
    }

    private void continueFetches(){
        switch(mState){
            case STATE_GET_ENTRANCES:
                if(mCurrQueryNum == 0) {
                    mCurrQueryNum++;
                    Log.d(MainActivity.TAG, "Fetching second station entrance");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_ENTRANCES, "api_key", API_URLS.WMATA_API_KEY,
                            "Lat", ""+mEndingAddr.latitude, "Lon", ""+mEndingAddr.longitude, "Radius", ""+HALF_MILE_IN_METERS);
                }else {
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_INFO;
                    //Run JSONfetcher (get lines)
                    Log.d(MainActivity.TAG, "Fetching first station lines");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mStartingStation.code);
                }
                break;
            case STATE_GET_STATION_INFO:
                if(mCurrQueryNum == 0){
                    mCurrQueryNum ++;
                    //Run JSONfetcher (second get startLine)
                    Log.d(MainActivity.TAG, "Fetching second station lines");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mEndingStation.code);
                }else{

                    //Run JSONfetcher (get station list)
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
                        //updateProgress(2);
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
                    if(!mEndingStation.altCode1.equals("")){
                        mCurrQueryNum ++;
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                                "StationCode", mEndingStation.altCode1);
                    }else{
                        //updateProgress(1);
                        mCurrQueryNum = 0;
                        mState = STATE_GET_STATION_LIST;

                        Log.d(MainActivity.TAG, "Fetching first station list");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                                "LineCode", lines[0]);
                    }
                }else {
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_LIST;

                    Log.d(MainActivity.TAG, "Fetching first station list");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", lines[0]);
                }
                break;
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum < lines.length-1){
                    mCurrQueryNum ++;
                    Log.d(MainActivity.TAG, "Fetching list for line: " + lines[mCurrQueryNum]);
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY, "LineCode", lines[mCurrQueryNum]);
                }
                break;
        }
    }


    private int getLinesIntersection(ArrayList<StationInfo> line1, ArrayList<StationInfo> line2, int startIndex){
        Log.d(MainActivity.TAG, "Finding intersection station...");
        for(int i = startIndex; i < line1.size(); i++)
            if(line2.contains(line1.get(i)))
                return i;

        Log.d(MainActivity.TAG, "Finding intersection station (iterating in reverse)...");
        for(int i = startIndex; i > 0; i--)
            if(line2.contains(line1.get(i)))
                return i;

        return -1;
    }

    private ArrayList<StationInfo> getTripLeg(ArrayList<StationInfo> line, int index1, int index2){
        if(index1 <= index2){
            return new ArrayList<StationInfo>(line.subList(index1,index2+1));
        }else{
            Log.d(MainActivity.TAG, "Returning reversed list");
            return new ArrayList<StationInfo>(line.subList(index2,index1+1));
        }
    }

    private ArrayList<String> getCommonLines(StationInfo startingStation, StationInfo endingStation){
        ArrayList<String> commonLines = new ArrayList<String>();

        for(String line : startingStation.lines)
            if(endingStation.lines.contains(line)) {
                commonLines.add(line);
                Log.d(MainActivity.TAG, "Common line: " + line);
            }

        return commonLines;
    }

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
            e.printStackTrace();
        }
        Log.d(MainActivity.TAG, "Found station: " + temp.name + ", lat: " + temp.latitude + ", lng: " + temp.longitude + ", code: " + temp.code);
        return temp;
    }

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
            e.printStackTrace();
        }

        return temp;
    }

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
            e.printStackTrace();
        }
        return lines;
    }

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
            e.printStackTrace();
        }

        for(StationInfo info : temp)
            Log.d(MainActivity.TAG, "Added, name: " + info.name + ", lat: " + info.latitude + ", lon: " + info.longitude + ", code: " + info.code
            + ", altCode1: " + info.altCode1 + ", altCode2: " + info.altCode2);
        return temp;
    }

    private Map<String, ArrayList<StationInfo>> parseAllStations(String jsonResult){
        Map<String, ArrayList<StationInfo>> temp = new HashMap<String, ArrayList<StationInfo>>();
        try {
            JSONObject topObject = new JSONObject(jsonResult);
            JSONArray stationsList = topObject.getJSONArray("Stations");
            String currLine;

            for(int i = 0; i < stationsList.length(); i++){
                JSONObject station = stationsList.getJSONObject(i);
                currLine = station.getString("LineCode1");

                if(!temp.containsKey(currLine))
                    temp.put(currLine, new ArrayList<StationInfo>());

                temp.get(currLine).add(new StationInfo(station.getString("Name"), station.getDouble("Lat"), station.getDouble("Lon"), station.getString("Code"),
                        station.getString("StationTogether1"), station.getString("StationTogether2")));
                Log.d(MainActivity.TAG, "Hashmap: " + currLine + ", " + station.getString("Name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return temp;
    }

    private void updateProgress(int amount){
        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, amount));
    }

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
