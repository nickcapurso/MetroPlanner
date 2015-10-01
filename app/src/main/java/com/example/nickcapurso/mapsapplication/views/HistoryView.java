package com.example.nickcapurso.mapsapplication.views;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.nickcapurso.mapsapplication.MainActivity;
import com.example.nickcapurso.mapsapplication.R;
import com.example.nickcapurso.mapsapplication.common.HandlerCodes;

/**
 * Custom view used to display trip history entries
 */
public class HistoryView extends LinearLayout{
    private static int PADDING_SMALL;
    private int id;

    private Context mContext;
    private TextView mTVStartingStation, mTVEndingStation, mTVDate;

    /**
     * Reference to the parent activity's handler
     */
    private Handler mClientHandler;


    /**
     * Sole constructor. Also takes in an ID and a reference to the parent activity (HistoryActivity)'s handler
     * to send a message to when the view is clicked.
     * @param context
     * @param startingStation
     * @param endingStation
     * @param date
     * @param id
     * @param handler
     */
    public HistoryView(Context context, String startingStation, String endingStation, String date, String id, Handler handler){
        super(context);
        mContext = context;
        PADDING_SMALL = (int) context.getResources().getDimension(R.dimen.padding_small);
        mClientHandler = handler;
        this.id = Integer.parseInt(id);
        createViews(startingStation, endingStation, date);
    }

    /**
     * Called to set up the individual views that comprise a HistoryView
     * @param startingStation
     * @param endingStation
     * @param date
     */
    private void createViews(String startingStation, String endingStation, String date){
        //Settings for the overall LinearLayout container.
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        setClickable(true);
        setBackground(mContext.getResources().getDrawable(R.drawable.white_button));
        setLayoutParams(parentParams);

        //TextView to hold the starting address
        mTVStartingStation = new TextView(mContext);
        LayoutParams linesParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVStartingStation.setText(startingStation);
        mTVStartingStation.setLayoutParams(linesParams);
        mTVStartingStation.setPadding(2*PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,0);

        //TextView to hold the ending address
        mTVEndingStation = new TextView(mContext);
        LayoutParams linesParams2 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVEndingStation.setText(endingStation);
        mTVEndingStation.setLayoutParams(linesParams2);
        mTVEndingStation.setPadding(2*PADDING_SMALL,PADDING_SMALL,0,0);

        //TextView to hold the date
        mTVDate = new TextView(mContext);
        LayoutParams linesParams3 = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVDate.setText(date);
        mTVDate.setLayoutParams(linesParams3);

        addView(mTVDate);
        addView(mTVStartingStation);
        addView(mTVEndingStation);

        //OnClick Listener sends the HistoryActivity a message when a HistoryView is clicked. The
        //message will contain the ID of the particular view
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.TAG, "onClick: Entry #"+id);
                mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.HISTORY_ENTRY_CHOSEN, id));
            }
        });
    }
}
