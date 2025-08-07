package com.majorware.echosms.data.models;

import java.io.Serializable;

import com.google.firebase.Timestamp;

public class SMS implements Serializable {

    private String id;
    private String title;
    private String message;
    private String category;
    private Timestamp createdAt;
    private String status;
    private String phoneNumber; // Nouvelle propriété

    // Constructeur vide requis pour Firestore
    public SMS() {
    }

    // Constructeur avec paramètres
    public SMS(String id, String title, String message, String category, Timestamp createdAt, String status, String phoneNumber) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.createdAt = createdAt;
        this.status = status;
        this.phoneNumber = phoneNumber;
    }

    // Getters et Setters


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
