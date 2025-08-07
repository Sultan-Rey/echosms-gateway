package com.majorware.echosms.data.repositories;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.majorware.echosms.R;
import com.majorware.echosms.data.tools.NetworkUtils;

public class FirebaseRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration firestoreListener;

    public void observeFirestoreConnection(
            Context context,
            EventListener<QuerySnapshot> listener,
            String licenseKey
    ) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            Toast.makeText(context, context.getResources().getString(R.string.offline), Toast.LENGTH_SHORT).show();
            Log.d("FirestoreRepository", "Pas d'accès à Internet !");
            return;
        }

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        firestoreListener = db.collection("echoLicense")
                .whereEqualTo("licenceKey", licenseKey)
                .limit(1)
                .addSnapshotListener(listener);
    }

    public void stopObservingFirestore() {
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
            Log.d("Firestore", "Écoute Firestore arrêtée.");
        }
    }
}

