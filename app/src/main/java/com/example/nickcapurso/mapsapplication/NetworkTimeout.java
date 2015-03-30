package com.example.nickcapurso.mapsapplication;

import android.os.CountDownTimer;
import android.os.Handler;

/**
 * Created by nickcapurso on 3/22/15.
 */
public class NetworkTimeout extends CountDownTimer {
    public static final long ONE_SECOND_MILLIS = 1000;
    public static final long DEFAULT_PERIOD = 10 * ONE_SECOND_MILLIS;
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

    @Override
    public void onFinish() {
        mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.TIMEOUT));
    }
}
