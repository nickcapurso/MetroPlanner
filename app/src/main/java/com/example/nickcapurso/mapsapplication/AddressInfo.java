package com.example.nickcapurso.mapsapplication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an address (or place of interest ex. "White House") and its longitude/latitude coordinates
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

    /**
     * Used to recreate an AddressInfo from a Parcel (ex. after sent between activities)
     * @param in
     */
    public AddressInfo(Parcel in){
        address = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Used to write the contents of an AddressInfo into parcel
     * @param dest The resulting parcel
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    //Formally used to call the constructor to recreate an AddressInfo from a parcel
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
