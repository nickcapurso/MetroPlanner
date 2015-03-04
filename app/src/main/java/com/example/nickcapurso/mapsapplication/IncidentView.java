
package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by nickcapurso on 3/4/15.
 */
public class IncidentView extends LinearLayout {
    private static int PADDING_SMALL;

    private Context mContext;
    private ImageView mIVIcon;
    private TextView mTVLines, mTVIncident;
    private LinearLayout mMessageContainer;
    private ScrollView mIncidentContainer;

    public IncidentView(Context context, String linesAffected, String incident) {
        super(context);
        this.mContext = context;
        PADDING_SMALL = (int) context.getResources().getDimension(R.dimen.padding_small);

        createViews(linesAffected, incident);
    }

    private void createViews(String linesAffected, String incident){
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setMargins(0,PADDING_SMALL,0,0);
        setOrientation(LinearLayout.HORIZONTAL);
        setPadding(PADDING_SMALL,PADDING_SMALL,PADDING_SMALL,PADDING_SMALL);
        setBackgroundColor(mContext.getResources().getColor(R.color.white));
        setLayoutParams(parentParams);


        mIVIcon = new ImageView(mContext);
        LayoutParams imageViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mIVIcon.setImageDrawable(mContext.getResources().getDrawable(android.R.drawable.ic_dialog_alert));
        mIVIcon.setLayoutParams(imageViewParams);

        mMessageContainer = new LinearLayout(mContext);
        LayoutParams msgCntrParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        msgCntrParams.setMargins(PADDING_SMALL,0,0,0);
        mMessageContainer.setOrientation(LinearLayout.VERTICAL);
        mMessageContainer.setLayoutParams(msgCntrParams);

        mTVLines = new TextView(mContext);
        LayoutParams linesParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVLines.setText(linesAffected);
        mTVLines.setLayoutParams(linesParams);

        mIncidentContainer = new ScrollView(mContext);
        mIncidentContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mTVIncident = new TextView(mContext);
        LayoutParams incidentParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        mTVIncident.setText(incident);
        mTVIncident.setLayoutParams(incidentParams);

        mIncidentContainer.addView(mTVIncident);
        mMessageContainer.addView(mTVLines);
        mMessageContainer.addView(mIncidentContainer);

        addView(mIVIcon);
        addView(mMessageContainer);
    }

}
