package com.example.nickcapurso.mapsapplication;

import android.os.CountDownTimer;
import android.os.Handler;

/**
 * Simple countdown timer for location and network requests. By default, the period is ten seconds.
 * When the timeout occurs, a message is sent to the "client" activity's handler.
 */
public class NetworkTimeout extends CountDownTimer {
    /**
     * One second
     */
    public static final long ONE_SECOND_MILLIS = 1000;

    /**
     * 10 * one second = 10 seconds
     */
    public static final long DEFAULT_PERIOD = 10 * ONE_SECOND_MILLIS;

    /**
     * Handler to send a message to when the time
     */
    private Handler mClientHandler;

    public NetworkTimeout(long millisInFuture, long countDownInterval, Handler clientHandler){
        super(millisInFuture, countDownInterval);
        mClientHandler = clientHandler;
    }

    public NetworkTimeout(Handler clientHandler){
        super(DEFAULT_PERIOD, ONE_SECOND_MILLIS);
        mClientHandler = clientHandler;
    }

    @Override
    public void onTick(long millisUntilFinished) { }

    /**
     * Send a timeout message to the client activity's handler when the timer finishes
     */
    @Override
    public void onFinish() {
        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.TIMEOUT));
    }
}
