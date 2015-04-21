package com.example.nickcapurso.mapsapplication;

import java.util.ArrayList;

/**
 * Represents a metro station (its name, longitude, latitude, code, lines, etc.)
 */
public class StationInfo {
    /**
     * The name of the station (ex. "Foggy Bottom)
     */
    public String name;

    /**
     * Longitude of the station
     */
    public double longitude;

    /**
     * Latitude of the station
     */
    public double latitude;

    /**
     * The station code of the station (from the Metro API)
     */
    public String code;

    /**
     * Alternative codes for the station (used when a station has multiple lines/platforms)
     */
    public String altCode1, altCode2;

    /**
     * The lines that pass through this station
     */
    public ArrayList<String> lines;

    public StationInfo(){
        lines = new ArrayList<String>();
        name = "";
        altCode1 = "";
        altCode2 = "";
    }

    public StationInfo(String name, double latitude, double longitude, String code){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.code = code;
        altCode1 = "";
        altCode2 = "";
        lines = new ArrayList<String>();
    }

    public StationInfo(String name, double latitude, double longitude, String code, String altCode1, String altCode2){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.code = code;
        this.altCode1 = altCode1;
        this.altCode2 = altCode2;
        lines = new ArrayList<String>();
    }

    /**
     * Overriden equals functions compares the names of the two stations
     * @param object
     * @return
     */
    @Override
    public boolean equals(Object object){
        if(object instanceof StationInfo){
            StationInfo station2 = (StationInfo)object;
            return name.equals(station2.name);
        }
        return false;
    }

    /**
     * Hash code of a station is the hash of its name (no two stations have the same name)
     * @return
     */
    @Override
    public int hashCode(){
        return name.hashCode();
    }
}
