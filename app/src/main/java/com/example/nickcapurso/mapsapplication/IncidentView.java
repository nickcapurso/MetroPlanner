
package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Custom view used to display metro incidents
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

    /**
     * Called to set up the individual views that comprise a IncidentView
     * @param incident
     */
    private void createViews(String incident){
        //Settings for the overall LinearLayout container.
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        setBackgroundColor(mContext.getResources().getColor(R.color.white));
        setLayoutParams(parentParams);

        //ImageView to display a "warning" icon
        mIVIcon = new ImageView(mContext);
        LayoutParams imageViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mIVIcon.setImageDrawable(mContext.getResources().getDrawable(android.R.drawable.ic_dialog_alert));
        mIVIcon.setLayoutParams(imageViewParams);

        //TextView to hold the actual metro incident
        mTVIncident = new TextView(mContext);
        LayoutParams incidentParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVIncident.setText(incident);
        mTVIncident.setPadding(2*PADDING_SMALL,0,0,0);
        mTVIncident.setLayoutParams(incidentParams);


        addView(mIVIcon);
        addView(mTVIncident);
    }
}
