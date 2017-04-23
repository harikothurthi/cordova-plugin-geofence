package com.cowbell.cordova.geofence;

import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class Profile {

    @Expose public String _id;
    @Expose public String fname;
    @Expose public String mname;
    @Expose public String lname;
    @Expose public String role;
    @Expose public String mfname;
    @Expose public String mlname;
    @Expose public String contact;
    @Expose public String preferredLocation;

    public Profile() {
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static Profile[] fromJsonArray(String json) {
        if (json == null) return null;
        Type collectionType = new TypeToken<Collection<Profile>>(){}.getType();
        Collection<Profile> profColl = Gson.get().fromJson(json, collectionType);
        Profile[] p = profColl.toArray(new Profile[profColl.size()]);
        return p;
    }

    public static Profile fromJson(String json){
        if (json == null) return null;
        return Gson.get().fromJson(json, Profile.class);
    }

    public List<NameValuePair> getParameterList(){
        DateFormat df = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("fname", fname));
        formparams.add(new BasicNameValuePair("mname", mname));
        formparams.add(new BasicNameValuePair("lname", lname));
        formparams.add(new BasicNameValuePair("mfname", mfname));
        formparams.add(new BasicNameValuePair("mlname", mlname));
        formparams.add(new BasicNameValuePair("contact", contact));
        formparams.add(new BasicNameValuePair("location", preferredLocation));
        formparams.add(new BasicNameValuePair("time", df.format(new Date())));
        return formparams;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Profile{" +
                "fname='" + fname + '\'' +
                ", mname='" + mname + '\'' +
                ", lname='" + lname + '\'' +
                ", role='" + role + '\'' +
                ", mfname='" + mfname + '\'' +
                ", mlname='" + mlname + '\'' +
                ", contact='" + contact + '\'' +
                ", preferredLocation='" + preferredLocation + '\'' +
                '}';
    }
}
