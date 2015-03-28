package com.example.nickcapurso.mapsapplication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class PlanningModule{
    private static final byte STATE_GET_ENTRANCES = 0;
    private static final byte STATE_GET_STATION_INFO = 1;
    private static final byte STATE_GET_ALT_LINES = 2;
    private static final byte STATE_GET_STATION_LIST = 3;
    private static final byte STATE_FINISHED = 4;
    private static final int HALF_MILE_IN_METERS = 805;
    private static final int ONE_SECOND = 1000;
    private static final int API_DELAY = ONE_SECOND / 2;

    private byte mState, mCurrQueryNum;
    private final AddressInfo mStartingAddr, mEndingAddr;
    private StationInfo mStartingStation, mEndingStation;
    private ArrayList<ArrayList<StationInfo>> mStartingLines, mEndingLines;
    private ArrayList<MetroPath> mPaths;
    private final Handler mClientHandler;

    public PlanningModule(AddressInfo startingAddr, AddressInfo endingAddr, Handler client){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
        mPaths = new ArrayList<MetroPath>();

        mStartingLines = new ArrayList<ArrayList<StationInfo>>();
        mEndingLines = new ArrayList<ArrayList<StationInfo>>();
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
            case STATE_GET_ENTRANCES:
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                if(mCurrQueryNum == 0) {
                    mStartingStation = parseStationEntrances(jsonResult);
                }else {
                    mEndingStation = parseStationEntrances(jsonResult);
                }
                break;
            case STATE_GET_STATION_INFO:
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                if(mCurrQueryNum == 0){
                    mStartingStation = parseStationInfo(jsonResult);
                }else{
                    mEndingStation = parseStationInfo(jsonResult);
                }
                break;
            case STATE_GET_ALT_LINES:
                if(mCurrQueryNum == 0) {
                    mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                    mStartingStation.lines = parseAltLines(mStartingStation.lines, jsonResult);
                }else{
                    mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                    mEndingStation.lines = parseAltLines(mEndingStation.lines, jsonResult);
                }
                break;
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum == 0){
                    //First station list fetched
                    mStartingLines.add( parseStationList(jsonResult));

                    //Check for  common lines
                    ArrayList<String> commonLines = getCommonLines(mStartingStation, mEndingStation);
                    if(commonLines.size() != 0){
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                        mState = STATE_FINISHED;

                        for(String s : commonLines)
                            mPaths.add(getPath(mStartingLines.get(0), s, mStartingStation, null, s, mEndingStation));
                    }
                }else{
                    mEndingLines.add( parseStationList(jsonResult));

                    //TODO put this stuff into a thread for every line
                    mPaths.add(getPath(mStartingLines.get(0), mStartingStation.lines.get(0), mStartingStation,
                            mEndingLines.get(0), mEndingStation.lines.get(0), mEndingStation));

                    //Second station list fetched
                    mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                    mState = STATE_FINISHED;
                }

                //If not same lines for both stations, run JSON fetcher again
                break;
        }
        if(mState != STATE_FINISHED) {
            mHandler.sendMessageDelayed(msg, API_DELAY);
            //mHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.FETCH_DELAY_DONE));
        }else {
            mClientHandler.sendMessageDelayed(mClientHandler.obtainMessage(HandlerCodes.PLANNING_MODULE_DONE, mPaths), API_DELAY);
        }

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
        }else{
            startIndex = line1.indexOf(startStation);
            endIndex = line2.indexOf(endStation);
            intersectionIndex1 = getLinesIntersection(line1, line2, startIndex);
            intersectionStation = line1.get(intersectionIndex1);
            intersectionIndex2 = line2.indexOf(intersectionStation);

            Log.d(MainActivity.TAG, "Intersection at: " + intersectionStation.name);


            path.startLine = line1Color;
            path.sameLine = false;
            path.endLine = line2Color;
            path.firstLeg = getTripLeg(line1, startIndex, intersectionIndex1);
            path.secondLeg = getTripLeg(line2, intersectionIndex2, endIndex);
        }


        return path;
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
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 2));
                        mCurrQueryNum = 0;
                        mState = STATE_GET_STATION_LIST;

                        Log.d(MainActivity.TAG, "Fetching first station list");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                                "LineCode", mStartingStation.lines.get(0));

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
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                        mCurrQueryNum = 0;
                        mState = STATE_GET_STATION_LIST;

                        Log.d(MainActivity.TAG, "Fetching first station list");
                        new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                                "LineCode", mStartingStation.lines.get(0));
                    }
                }else{
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_LIST;

                    Log.d(MainActivity.TAG, "Fetching first station list");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", mStartingStation.lines.get(0));
                }
                break;
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum == 0){


                    mCurrQueryNum ++;
                    //Run JSONfetcher for the second startLine (get station list)
                    Log.d(MainActivity.TAG, "Fetching second station list");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", mEndingStation.lines.get(0));
                }
                break;
        }
    }


    private int getLinesIntersection(ArrayList<StationInfo> line1, ArrayList<StationInfo> line2, int startIndex){
        Log.d(MainActivity.TAG, "Finding intersection station...");
        for(int i = startIndex; i < mStartingLines.get(0).size(); i++)
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
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(StationInfo info : temp)
            Log.d(MainActivity.TAG, "Added, name: " + info.name + ", lat: " + info.latitude + ", lon: " + info.longitude + ", code: " + info.code
            + ", altCode1: " + info.altCode1 + ", altCode2: " + info.altCode2);
        return temp;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            Log.d(MainActivity.TAG, "PlanningModule, message received ("+message.what+")");
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    parseFetchResults((String) message.obj);
                    break;
                case HandlerCodes.FETCH_DELAY_DONE:
                    continueFetches();
                    break;
            }
        }
    };

    //TODO add advanceProgressDialog()
}
