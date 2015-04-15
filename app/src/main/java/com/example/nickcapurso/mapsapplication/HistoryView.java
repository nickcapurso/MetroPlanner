package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by cheng on 4/14/15.
 */
public class HistoryView extends LinearLayout{
    private static int PADDING_SMALL;
    private int id;

    private Context mContext;
    private Handler mClientHandler;
    private TextView mTVStartingStation, mTVEndingStation, mTVDate;

    public HistoryView(Context context, String startingStation, String endingStation, String date, String id, Handler handler){
        super(context);
        mContext = context;
        PADDING_SMALL = (int) context.getResources().getDimension(R.dimen.padding_small);
        mClientHandler = handler;
        this.id = Integer.parseInt(id);
        createViews(startingStation, endingStation, date);
    }

    private void createViews(String startingStation, String endingStation, String date){
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
       // setBackgroundColor(mContext.getResources().getColor(R.color.white));
        setClickable(true);
        setBackground(mContext.getResources().getDrawable(R.drawable.white_button));
        setLayoutParams(parentParams);

        mTVStartingStation = new TextView(mContext);
        LayoutParams linesParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVStartingStation.setText(startingStation);
        mTVStartingStation.setLayoutParams(linesParams);
        mTVStartingStation.setPadding(2*PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,0);

        mTVEndingStation = new TextView(mContext);
        LayoutParams linesParams2 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVEndingStation.setText(endingStation);
        mTVEndingStation.setLayoutParams(linesParams2);
        mTVEndingStation.setPadding(2*PADDING_SMALL,PADDING_SMALL,0,0);

        mTVDate = new TextView(mContext);
        LayoutParams linesParams3 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVDate.setText(date);
        mTVDate.setLayoutParams(linesParams3);

        addView(mTVDate);
        addView(mTVStartingStation);
        addView(mTVEndingStation);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.TAG, "onClick: Entry #"+id);
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.HISTORY_ENTRY_CHOSEN, id));
            }
        });
    }
}
