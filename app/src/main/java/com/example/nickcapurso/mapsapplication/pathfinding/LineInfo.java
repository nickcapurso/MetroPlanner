package com.example.nickcapurso.mapsapplication.pathfinding;

/**
 * Represents a line (color and starting/ending stations)
 */
public class LineInfo {
    /**
     * Two letter color code.
     */
    public String color;

    /**
     * Station code for the "starting" station
     */
    public String startStationCode;

    /**
     * Station code for the usual "ending" station
     */
    public String endStationCode;

    public LineInfo(String color, String startStationCode, String endStationCode){
        this.color = color;
        this.startStationCode = startStationCode;
        this.endStationCode = endStationCode;
    }

    /**
     * Two lines are equal if their colors match
     * @param object
     * @return
     */
    @Override
    public boolean equals(Object object){
        if(object instanceof LineInfo){
            LineInfo line2 = (LineInfo)object;
            return color.equals(line2.color);
        }
        return false;
    }
}
