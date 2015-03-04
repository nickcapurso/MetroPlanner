package com.example.nickcapurso.mapsapplication;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by nickcapurso on 3/4/15.
 */
public class ShadowView extends View{

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
