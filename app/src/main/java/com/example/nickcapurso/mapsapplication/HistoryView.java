package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by cheng on 4/14/15.
 */
public class HistoryView extends LinearLayout{
    private static int PADDING_SMALL;

    private Context mContext;
    private TextView mTVStartingStation, mTVEndingStation, mTVDate;

    public HistoryView(Context context, String startingStation, String endingStation, String date){
        super(context);
        mContext = context;
        PADDING_SMALL = (int) context.getResources().getDimension(R.dimen.padding_small);
        createViews(startingStation, endingStation, date);
    }

    private void createViews(String startingStation, String endingStation, String date){
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        setBackgroundColor(mContext.getResources().getColor(R.color.white));
        setLayoutParams(parentParams);

        mTVStartingStation = new TextView(mContext);
        LayoutParams linesParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVStartingStation.setText(startingStation);
        mTVStartingStation.setLayoutParams(linesParams);
        mTVStartingStation.setPadding(PADDING_SMALL,0,0,0);

        mTVEndingStation = new TextView(mContext);
        LayoutParams linesParams2 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVEndingStation.setText(endingStation);
        mTVEndingStation.setLayoutParams(linesParams2);
        mTVEndingStation.setPadding(PADDING_SMALL,0,0,0);

        mTVDate = new TextView(mContext);
        LayoutParams linesParams3 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVDate.setText(date);
        mTVDate.setLayoutParams(linesParams3);

        addView(mTVDate);
        addView(mTVStartingStation);
        addView(mTVEndingStation);
    }
}
