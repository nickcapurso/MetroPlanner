package com.example.nickcapurso.mapsapplication;

import java.util.ArrayList;

/**
 * Created by cheng on 3/27/15.
 */
public class MetroPath {
    public ArrayList<StationInfo> firstLeg, secondLeg, thirdLeg;
    public ArrayList<String> firstLegSharedLines, secondLegSharedLines, thirdLegSharedLines;
    public String startLine, endLine;
    public boolean sameLine;


    public MetroPath(){
        firstLeg = new ArrayList<StationInfo>();
        secondLeg = new ArrayList<StationInfo>();
        thirdLeg = new ArrayList<StationInfo>();
        firstLegSharedLines = new ArrayList<String>();
        secondLegSharedLines = new ArrayList<String>();
        thirdLegSharedLines = new ArrayList<String>();
    }
}