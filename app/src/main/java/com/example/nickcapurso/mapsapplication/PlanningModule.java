package com.example.nickcapurso.mapsapplication;

import android.os.Handler;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class PlanningModule {
    public static final byte PLANNING_COMPLETE = 10;

    private final String mStartingAddr, mEndingAddr;
    private final Handler mClientHandler;

    public PlanningModule(String startingAddr, String endingAddr, Handler client){
        mStartingAddr = startingAddr;
        mEndingAddr = endingAddr;
        mClientHandler = client;
    }
}
