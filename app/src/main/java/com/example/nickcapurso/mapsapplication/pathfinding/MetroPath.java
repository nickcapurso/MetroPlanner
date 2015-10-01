package com.example.nickcapurso.mapsapplication.pathfinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for a metro path planned by the Planning Module.
 */
public class MetroPath {
    /**
     * List of stations that comprise the first leg of the trip
     */
    public ArrayList<StationInfo> firstLeg;

    /**
     * List of stations that comprise the second leg of the trip
     */
    public ArrayList<StationInfo> secondLeg;

    /**
     * Map of key-value pairs where a line is associated with the station that the user should
     * take the line "towards" (ex. take the Orange line towards Vienna => <OR, Vienna>
     */
    public Map<String, String> lineTowards = new HashMap<String, String>();

    /**
     * The color (line) of the first leg of the line
     */
    public String startLine;

    /**
     * The color (line) of the second leg of the line
     */
    public String endLine;

    /**
     * Set to true if the trip is direct (i.e. starting and ending stations are on the same line)
     */
    public boolean sameLine;

    /**
     * The index within firstLeg where the starting station lies
     */
    public int startIndex;

    /**
     * The index within firstLeg where the ending station lies
     */
    public int endIndex;

    /**
     * Initializes the leg ArrayLists
     */
    public MetroPath(){
        firstLeg = new ArrayList<StationInfo>();
        secondLeg = new ArrayList<StationInfo>();
    }
}
