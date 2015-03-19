package com.example.nickcapurso.mapsapplication;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class PlanningModule{
    public static final byte PLANNING_COMPLETE = 10;

    private byte state;
    private final String mStartingAddr, mEndingAddr;
    private final Handler mClientHandler;
    private ProgressDialog mDialog;

    public PlanningModule(String startingAddr, String endingAddr, Handler client, ProgressDialog loadingDialog){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
        mDialog = loadingDialog;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            Log.d(MainActivity.TAG, "PlanningModule, message received ("+message.what+")");
            switch(message.what){
                case HandlerCodes.JSON_FETCH_DONE:
                    break;
            }
        }
    };
}
