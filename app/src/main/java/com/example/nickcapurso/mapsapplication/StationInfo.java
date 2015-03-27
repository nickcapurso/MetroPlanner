package com.example.nickcapurso.mapsapplication;

import java.util.ArrayList;

/**
 * Created by nickcapurso on 3/19/15.
 */
public class StationInfo {
    public String name;
    public double longitude;
    public double latitude;
    public String code;
    public String altCode1, altCode2;
    public ArrayList<String> lines;

    public StationInfo(){
        lines = new ArrayList<String>();
    }
    public StationInfo(String name, double latitude, double longitude, String code){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.code = code;
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

    @Override
    public boolean equals(Object object){
        if(object instanceof StationInfo){
            StationInfo station2 = (StationInfo)object;
            return code.equals(station2.code);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return code.hashCode();
    }
}
