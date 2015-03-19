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
}
