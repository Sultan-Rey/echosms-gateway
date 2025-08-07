package com.majorware.echosms.data.models;

public class ServicesAttribution {
    private boolean isLaunch = false;
    private String fireCollectionSet = "";
    private String licenseKey = "";
    private final int sendingAttempts;


    public ServicesAttribution( boolean isLaunch, String fireCollectionSet, String licenseKey, int sendingAttempts) {
        this.isLaunch = isLaunch;
        this.fireCollectionSet = fireCollectionSet;
        this.licenseKey = licenseKey;
        this.sendingAttempts = sendingAttempts;
    }

    public int getSendingAttempts() {
        return sendingAttempts;
    }

    public boolean isLaunch() {
        return isLaunch;
    }

    public String getFireCollectionSet() {
        return fireCollectionSet;
    }

    public String getLicenseKey() {
        return licenseKey;
    }




}
