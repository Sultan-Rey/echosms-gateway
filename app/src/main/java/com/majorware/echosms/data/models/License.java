package com.majorware.echosms.data.models;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class License implements Serializable {
    private String benefits;
    private String plan;
    private String subscriptionDelay;
    private String licenceKey;
    private String deviceId;
    private Timestamp purchaseDate;
    private Timestamp expiryDate;
    private boolean isActive;

    private double cost;

    public License(String benefits, String plan, String subscriptionDelay, String licenceKey, String deviceId, Timestamp purchaseDate, Timestamp expiryDate, boolean isActive, double cost) {
        this.benefits = benefits;
        this.plan = plan;
        this.subscriptionDelay = subscriptionDelay;
        this.licenceKey = licenceKey;
        this.deviceId = deviceId;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.cost = cost;
    }
    public License() {
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getSubscriptionDelay() {
        return subscriptionDelay;
    }

    public void setSubscriptionDelay(String subscriptionDelay) {
        this.subscriptionDelay = subscriptionDelay;
    }

    public String getLicenceKey() {
        return licenceKey;
    }

    public void setLicenceKey(String licenceKey) {
        this.licenceKey = licenceKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Timestamp getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Timestamp purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Timestamp getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Timestamp expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}

