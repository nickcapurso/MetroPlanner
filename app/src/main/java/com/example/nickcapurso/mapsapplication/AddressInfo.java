package com.example.nickcapurso.mapsapplication;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class AddressInfo {
    public String address;
    public double latitude;
    public double longitude;

    public AddressInfo(String address, double latitude, double longitude){
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
