
package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by nickcapurso on 3/4/15.
 */
public class IncidentView extends LinearLayout {
    private static int PADDING_SMALL;

    private Context mContext;
    private ImageView mIVIcon;
    private TextView mTVIncident;

    public IncidentView(Context context, String incident) {
        super(context);
        mContext = context;
        PADDING_SMALL = (int) context.getResources().getDimension(R.dimen.padding_small);

        createViews(incident);
    }

    private void createViews(String incident){
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        setBackgroundColor(mContext.getResources().getColor(R.color.white));
        setLayoutParams(parentParams);


        mIVIcon = new ImageView(mContext);
        LayoutParams imageViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mIVIcon.setImageDrawable(mContext.getResources().getDrawable(android.R.drawable.ic_dialog_alert));
        mIVIcon.setLayoutParams(imageViewParams);

        mTVIncident = new TextView(mContext);
        LayoutParams incidentParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVIncident.setText(incident);
        mTVIncident.setPadding(2*PADDING_SMALL,0,0,0);
        mTVIncident.setLayoutParams(incidentParams);


        addView(mIVIcon);
        addView(mTVIncident);
    }


    public String getLine(String code){
        String line = "";

        switch(code){
            case "RD":
                line = "Red";
                break;
            case "BL":
                line = "Blue";
                break;
            case "OR":
                line = "Orange";
                break;
            case "SV":
                line = "Silver";
                break;
            case "YL":
                line = "Yellow";
                break;
            case "GR":
                line = "Green";
                break;
        }
        return line;
    }
}
