package com.tommihirvonen.exifnotes.Datastructures;

// Copyright 2015
// Tommi Hirvonen

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The camera class holds the information of a camera.
 */
public class Camera implements Parcelable {

    private long id;
    private String make;
    private String model;
    private String serialNumber;
    private String minShutter;
    private String maxShutter;

    public Camera(){

    }

    public Camera(long id, String make, String model, String serialNumber, String minShutter, String maxShutter){
        this.id = id;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.minShutter = minShutter;
        this.maxShutter = maxShutter;
    }

    public void setId(long input){
        this.id = input;
    }

    public void setMake(String input){
        this.make = input;
    }

    public void setModel(String input){
        this.model = input;
    }

    public void setSerialNumber(String input){
        this.serialNumber = input;
    }

    public void setMinShutter(String input){
        this.minShutter = input;
    }

    public void setMaxShutter(String input){
        this.maxShutter = input;
    }


    public long getId(){
        return this.id;
    }

    public String getMake(){
        return this.make;
    }

    public String getModel(){
        return this.model;
    }

    public String getSerialNumber(){
        return this.serialNumber;
    }

    public String getMinShutter(){
        return this.minShutter;
    }

    public String getMaxShutter(){
        return this.maxShutter;
    }

    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    public Camera(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
        this.serialNumber = pc.readString();
        this.minShutter = pc.readString();
        this.maxShutter = pc.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(make);
        parcel.writeString(model);
        parcel.writeString(serialNumber);
        parcel.writeString(minShutter);
        parcel.writeString(maxShutter);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<Camera> CREATOR = new Parcelable.Creator<Camera>() {
        public Camera createFromParcel(Parcel pc) {
            return new Camera(pc);
        }
        public Camera[] newArray(int size) {
            return new Camera[size];
        }
    };

}
