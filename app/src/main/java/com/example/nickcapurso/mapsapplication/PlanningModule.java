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
    public static final byte PLANNING_COMPLETE = 10;

    private static final byte STATE_GET_ENTRANCES = 0;
    private static final byte STATE_GET_LINES = 1;
    private static final byte STATE_GET_STATION_LIST = 2;
    private static final byte STATE_FINISHED = 3;
    private static final int HALF_MILE_IN_METERS = 805;
    private static final int ONE_SECOND = 1000;
    private static final int API_DELAY = ONE_SECOND / 2;

    private byte mState, mCurrQueryNum, mCurrPath;
    private boolean mSameLine;
    private final AddressInfo mStartingAddr, mEndingAddr;
    private StationInfo mStartingStation, mEndingStation, mMiddleStation;
    private ArrayList<MetroPath> mPaths;
    private ArrayList<String> mCommonLines;
    private final Handler mClientHandler;

    public PlanningModule(AddressInfo startingAddr, AddressInfo endingAddr, Handler client){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
        mPaths = new ArrayList<MetroPath>();
    }

    public void start(){
        mState = STATE_GET_ENTRANCES;
        mCurrQueryNum = mCurrPath = 0;
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
                    mStartingStation = parseStation(jsonResult);
                }else {
                    mEndingStation = parseStation(jsonResult);
                }
                break;
            case STATE_GET_LINES:
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                if(mCurrQueryNum == 0){
                    mStartingStation.lines = parseStationLines(jsonResult);
                }else{
                    //Get second station's lines
                    mEndingStation.lines = parseStationLines(jsonResult);

                    //---------------------------------------------------------------------------------------------
                    mCommonLines = getCommonLines(mStartingStation, mEndingStation);

                    if(mCommonLines.size() == 0){
                        Log.d(MainActivity.TAG, "No Common Lines");
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 1));
                    }else{
                        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.UPDATE_PROGRESS, 2));
                        mState = STATE_FINISHED;

                        for(int i = 0; i < mCommonLines.size(); i++){
                            MetroPath path = new MetroPath();
                            path.startLine = mCommonLines.get(i);
                            path.sameLine = true;
                            path.startStation = mStartingStation;
                            path.endStation = mEndingStation;
                            mPaths.add(path);
                        }
                    }
                }
                break;

            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum == 0){
                    //First station list fetched
                }else{
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
                    mState = STATE_GET_LINES;
                    //Run JSONfetcher (get lines)
                    //TODO fetch lines based on alternate station codes
                    Log.d(MainActivity.TAG, "Fetching first station lines");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mStartingStation.code);
                }
                break;
            case STATE_GET_LINES:
                if(mCurrQueryNum == 0){
                    mCurrQueryNum ++;
                    //Run JSONfetcher (second get startLine)
                    //TODO fetch lines based on alternate station codes
                    Log.d(MainActivity.TAG, "Fetching second station lines");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_INFO, "api_key", API_URLS.WMATA_API_KEY,
                            "StationCode", mEndingStation.code);
                }else{
                    mCurrQueryNum = 0;
                    mState = STATE_GET_STATION_LIST;




                    //Run JSONfetcher (get station list)
                    Log.d(MainActivity.TAG, "Fetching first station list");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", mStartingStation.lines.get(0));
                }
                break;
            case STATE_GET_STATION_LIST:
                if(mCurrQueryNum == 0){


                    mCurrQueryNum ++;
                    //Run JSONfetcher for the second startLine (get station list)
                    Log.d(MainActivity.TAG, "Fetching second station lines");
                    new JSONFetcher(mHandler).execute(API_URLS.STATION_LIST, "api_key", API_URLS.WMATA_API_KEY,
                            "LineCode", mEndingStation.lines.get(0));
                }else{

                }
                break;
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

    private StationInfo parseStation(String jsonResult){
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

    private ArrayList<String> parseStationLines(String jsonResult){
        ArrayList<String> temp = new ArrayList<String>();
        try {
            JSONObject topObject = new JSONObject(jsonResult);
            String line = "";

            if(!topObject.isNull("LineCode1"))
                temp.add(topObject.getString("LineCode1"));

            if(!topObject.isNull("LineCode2"))
                temp.add(topObject.getString("LineCode2"));

            if(!topObject.isNull("LineCode3"))
                temp.add(topObject.getString("LineCode3"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(String s : temp)
            Log.d(MainActivity.TAG, "Added line: " + s);
        return temp;
    }

    private ArrayList<StationInfo> parseStationList(String jsonResult){
        ArrayList<StationInfo> temp = new ArrayList<StationInfo>();

        try {
            JSONObject topObject = new JSONObject(jsonResult);
            JSONArray stationsList = topObject.getJSONArray("Stations");

            for(int i = 0; i < stationsList.length(); i++){
                JSONObject station = stationsList.getJSONObject(i);
                //TODO add StationTogether codes (ex. Metro Center is A01 (red startLine) and C01 (silver/orange/blue)
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
}
