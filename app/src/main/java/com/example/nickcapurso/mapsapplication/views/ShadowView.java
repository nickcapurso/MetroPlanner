package com.example.nickcapurso.mapsapplication.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.example.nickcapurso.mapsapplication.R;

/**
 * Custom view used to display a "shadow" under other views (like IncidentView and HistoryView)
 */
public class ShadowView extends View{

    /**
     * Simple view of a fixed height (dependent on the pixel density of the screen), colored grey.
     * @param context
     */
    public ShadowView(Context context) {
        super(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0);

        //Conversion from pixels to device independent pixels
        float scale = context.getResources().getDisplayMetrics().density;
        params.height = (int) (2 * scale + 0.5f);

        setLayoutParams(params);
        setBackgroundColor(context.getResources().getColor(R.color.shadow));
    }
}
