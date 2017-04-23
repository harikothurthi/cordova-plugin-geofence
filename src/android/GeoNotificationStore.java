package com.cowbell.cordova.geofence;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;


public class GeoNotificationStore {

    private LocalStorage storage;

    public GeoNotificationStore(Context context) {
        storage = new LocalStorage(context);
    }

    public void setGeoNotification(GeoNotification geoNotification) {
        storage.setItem(geoNotification.id, Gson.get().toJson(geoNotification));
    }

    public GeoNotification getGeoNotification(String id) {
        String objectJson = storage.getItem(id);
        return GeoNotification.fromJson(objectJson);
    }

    public void setProfile(Profile profile) {
        storage.setProfile("1", Gson.get().toJson(profile));
    }

    public Profile getProfile() {
        String objectJson = storage.getProfile("1");
        return Profile.fromJson(objectJson);
    }

    public String getProfileString(){
        String objectJson = storage.getProfile("1");
        return objectJson;
    }

    public void removeProfile() {
        storage.removeProfile();
    }

    public void setSISO(SISO siso) {
        storage.setItem(SISO.ID, Gson.get().toJson(siso));
    }

    public SISO getSISO(String id) {
        String objectJson = storage.getItem(id);
        return SISO.fromJson(objectJson);
    }

    public List<String> getAllString() {
        return storage.getAllItems();
    }

    public List<GeoNotification> getAll() {
        List<String> objectJsonList = storage.getAllItems();
        List<GeoNotification> result = new ArrayList<GeoNotification>();
        for (String json : objectJsonList) {
            result.add(GeoNotification.fromJson(json));
        }
        return result;
    }

    public void remove(String id) {
        storage.removeItem(id);
    }

    public void clear() {
        storage.clear();
    }
}
