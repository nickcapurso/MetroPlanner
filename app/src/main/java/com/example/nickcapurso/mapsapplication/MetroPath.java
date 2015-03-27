package com.example.nickcapurso.mapsapplication;

import java.util.ArrayList;

/**
 * Created by cheng on 3/27/15.
 */
public class MetroPath {
    ArrayList<StationInfo> firstLeg, secondLeg;
    StationInfo intersection;

    public MetroPath(){
        firstLeg = new ArrayList<StationInfo>();
        secondLeg = new ArrayList<StationInfo>();
        intersection = new StationInfo();
    }
}
