package com.cowbell.cordova.geofence;

import java.net.URI;

public class OperationParams {
    private OperationType operationType;
    private URI URL;
    private Profile profile;

    public OperationParams() {
    }

    public OperationParams(OperationType operationType, URI URL) {
        this.operationType = operationType;
        this.URL = URL;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public URI getURL() {
        return URL;
    }

    public void setURL(URI URL) {
        this.URL = URL;
    }

    public Profile getUserProfile() {
        return profile;
    }

    public void setUserProfile(Profile profile) {
        this.profile = profile;
    }
}