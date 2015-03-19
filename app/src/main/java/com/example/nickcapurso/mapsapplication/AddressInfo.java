package com.example.nickcapurso.mapsapplication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by nickcapurso on 3/18/15.
 */
public class AddressInfo implements Parcelable {
    public String address;
    public double latitude;
    public double longitude;

    public AddressInfo(String address, double latitude, double longitude){
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public AddressInfo(Parcel in){
        address = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public AddressInfo createFromParcel(Parcel source) {
            return new AddressInfo(source);
        }

        @Override
        public AddressInfo[] newArray(int size) {
            return new AddressInfo[size];
        }
    };
}
