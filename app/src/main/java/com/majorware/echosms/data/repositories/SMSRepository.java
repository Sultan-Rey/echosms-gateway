package com.majorware.echosms.data.repositories;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.majorware.echosms.data.interfaces.SMSListCallback;
import com.majorware.echosms.data.interfaces.SMSListener;
import com.majorware.echosms.data.models.SMS;
import com.majorware.echosms.data.models.ServicesAttribution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SMSRepository {
    private static final String TAG = "SMSRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final SMSRepository instance = new SMSRepository();
    private ListenerRegistration smsListener;
    private final MutableLiveData<ServicesAttribution> servicesAttribution = new MutableLiveData<>();

    public MutableLiveData<ServicesAttribution> getServicesAttribution() {
        return servicesAttribution;
    }

    public static SMSRepository getInstance() {
        return instance;
    }

    public void updateLaunchStatus(boolean isLaunch) {
        ServicesAttribution current = servicesAttribution.getValue();
        if (current != null) {
            servicesAttribution.postValue(new ServicesAttribution(
                    isLaunch,
                    current.getFireCollectionSet(),
                    current.getLicenseKey(),
                    current.getSendingAttempts()
            ));
        }
    }

    public void setInitialAttribution(ServicesAttribution sa) {
        servicesAttribution.postValue(sa);
    }
    public void getAllSMS(final String collectionName, final OnSuccessListener<List<SMS>> onSuccess, final OnFailureListener onFailure) {

        if (collectionName == null || collectionName.trim().isEmpty()) {
            Log.d(TAG, "Nom de la collection Firestore vide, recuperation annulée !");
            return;
        }

        db.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SMS> smsList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Créer l'objet SMS à partir du document
                        SMS sms = document.toObject(SMS.class);

                        // Récupérer l'ID du document et le passer à la propriété id de SMS
                        if (sms != null) {
                            sms.setId(document.getId());  // Assurez-vous que la méthode setId existe dans votre classe SMS
                        }

                        // Ajouter l'objet SMS à la liste
                        smsList.add(sms);
                    }
                    // Passer la liste à l'OnSuccessListener
                    onSuccess.onSuccess(smsList);
                })
                .addOnFailureListener(onFailure);
    }
    public void getRecentSMSLogs(SMSListCallback callback, final String collectionName) {
        Timestamp thirtyMinutesAgo = new Timestamp(Timestamp.now().getSeconds() - (30 * 60), 0);
        if(collectionName == null || collectionName.isEmpty()) return;
        db.collection(collectionName)
                .whereGreaterThan("createdAt", thirtyMinutesAgo)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    List<SMS> smsList = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            SMS sms = document.toObject(SMS.class);
                            smsList.add(sms);
                        }
                    }
                    callback.onResult(smsList);
                });
    }
    public void updateSMSStatus(String SMS_ID, String status) {
        // Mettez à jour le statut dans Firestore avec le statut donné ("processing", "sent", "failed")
        db.collection("topcashSMS")
                .document(SMS_ID) // Utiliser un identifiant unique
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Statut mis à jour : " + status);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Échec de la mise à jour du statut", e);
                });
    }
    public void listenToSMSUpdates(SMSListener callback, final String collectionName) {
        if (collectionName == null || collectionName.trim().isEmpty()) {
            Log.e(TAG, "Nom de la collection Firestore vide, écoute annulée !");
            return;
        }

        CollectionReference smsCollection = db.collection(collectionName);

        this.smsListener =  smsCollection.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Erreur d'écoute Firestore", error);
                return;
            }

            if (snapshots != null && !snapshots.isEmpty()) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            callback.onSMSAdded();
                            break;
                        case MODIFIED:
                            callback.onSMSModified();
                            break;
                    }
                }
            }
        });
    }
    public void stopListeningToSMSUpdates() {
        if (smsListener != null) {
            smsListener.remove();
            smsListener = null;
            Log.d("FirestoreListener", "Écoute Firestore arrêtée !");
        }
    }

    public void incrementRetryCount(String SMS_ID) {
        db.collection("sms")
            .document(SMS_ID)
            .update("retryCount", FieldValue.increment(1));
    }

    public int getRetryCount(String SMS_ID) {
        DocumentReference docRef = db.collection("sms").document(SMS_ID);
        DocumentSnapshot document = docRef.get().getResult();
        if (document.exists()) {
            return document.getLong("retryCount") != null ? document.getLong("retryCount").intValue() : 0;
        }
        return 0;
    }
}
