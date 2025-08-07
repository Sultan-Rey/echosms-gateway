package com.majorware.echosms.data.repositories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.majorware.echosms.R;
import com.majorware.echosms.data.interfaces.OnLicenseValidationListener;
import com.majorware.echosms.data.models.License;

import java.util.Date;

public class LicenseRepository {
    private static final String TAG = "LicenceRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void validateLicense(String licenseKey, Context context, OnLicenseValidationListener listener) {
        if (context == null || listener == null) {
        Log.e(TAG, "Context ou listener null");
        return;
    }
    if (licenseKey == null || licenseKey.trim().isEmpty()) {
        listener.onValidationFailed(context.getString(R.string.msg_license_invalid));
        return;
    }

        @SuppressLint("HardwareIds") String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        SharedPreferences prefs = context.getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        db.collection("echoLicense")
                .whereEqualTo("licenceKey", licenseKey)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String plan = document.getString("plan");
                        Timestamp expiryDate = document.getTimestamp("expiryDate");
                        boolean isActive = document.getBoolean("isActive") != null && Boolean.TRUE.equals(document.getBoolean("isActive"));
                        String storedDeviceId = document.getString("deviceId");

                        // Vérifier si la licence est expirée
                        if (expiryDate != null && expiryDate.toDate().before(new Date())) {
                            db.collection("echoLicense").document(document.getId())
                                    .update("isActive", false)
                                    .addOnSuccessListener(aVoid -> {
                                        listener.onValidationFailed(context.getString(R.string.msg_license_expired));
                                        editor.putBoolean("isLicenseValidated", false).apply();
                                    })
                                    .addOnFailureListener(e -> listener.onValidationFailed(context.getString(R.string.msg_license_update_failed)));
                            return;
                        }

                        // Pour les plans d'essai (trial), autoriser plusieurs appareils
                        if (plan != null && plan.equalsIgnoreCase("trial")) {
                            if (isActive) {
                                listener.onValidationSuccess(true);
                                editor.putBoolean("isLicenseValidated", true).apply();
                            } else {
                                listener.onValidationFailed(context.getString(R.string.msg_license_validation_failed));
                                editor.putBoolean("isLicenseValidated", false).apply();
                            }
                        } else {
                            // Pour les autres plans, maintenir la logique existante
                            if (isActive && (storedDeviceId == null || storedDeviceId.isEmpty())) {
                                db.collection("echoLicense").document(document.getId())
                                        .update("deviceId", deviceId)
                                        .addOnSuccessListener(aVoid -> {
                                            listener.onValidationSuccess(true);
                                            editor.putBoolean("isLicenseValidated", true).apply();
                                        })
                                        .addOnFailureListener(e -> listener.onValidationFailed(context.getString(R.string.msg_license_update_failed)));
                            } else {
                                listener.onValidationFailed(context.getString(R.string.msg_license_validation_failed));
                                editor.putBoolean("isLicenseValidated", false).apply();
                            }
                        }
                    } else {
                        listener.onValidationFailed(context.getString(R.string.msg_license_invalid));
                        editor.putBoolean("isLicenseValidated", false).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onValidationFailed(context.getString(R.string.msg_license_error));
                    editor.putBoolean("isLicenseValidated", false).apply();
                });
    }
    public void getCurrentLicense(final OnSuccessListener<License> onSuccess,
                                  final OnFailureListener onFailure,
                                  String licenseKey) {



        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            Log.d(TAG, "Clé de licence vide, récupération annulée !");
            onFailure.onFailure(new Exception("Clé de licence invalide."));
            return;
        }

        db.collection("echoLicense")
                .whereEqualTo("licenceKey", licenseKey)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        License license = document.toObject(License.class);

                        if (license != null) {
                            onSuccess.onSuccess(license);
                        } else {
                            Log.d("FirestoreListener", "Impossible de convertir le document en License.");
                            onFailure.onFailure(new Exception("Erreur de conversion du document en License."));
                        }
                    } else {
                        Log.d("FirestoreListener", "Aucune licence trouvée pour la clé fournie.");
                        onFailure.onFailure(new Exception("Aucune licence trouvée."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("FirestoreListener", "Erreur lors de la récupération de la licence : " + e.getMessage());
                    onFailure.onFailure(e);
                });
    }
}
