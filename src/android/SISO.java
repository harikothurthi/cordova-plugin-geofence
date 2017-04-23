package com.cowbell.cordova.geofence;

import com.google.gson.annotations.Expose;

public class SISO {
    @Expose public static String ID;
    @Expose public String _id;
    @Expose public String fname;
    @Expose public String mname;
    @Expose public String lname;
    @Expose public String mfname;
    @Expose public String mlname;
    @Expose public String contact;
    @Expose public String location;
    @Expose public String time;
    @Expose public String __v;
    @Expose public String cancelled;
    @Expose public String date;


    public SISO() {
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static SISO fromJson(String json) {
        if (json == null) return null;
        return Gson.get().fromJson(json, SISO.class);
    }
}
