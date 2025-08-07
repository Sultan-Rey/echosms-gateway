package com.majorware.echosms.data.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.majorware.echosms.MainActivity;
import com.majorware.echosms.R;
import com.majorware.echosms.data.interfaces.OnLicenseValidationListener;
import com.majorware.echosms.data.interfaces.SMSListener;
import com.majorware.echosms.data.models.SMS;
import com.majorware.echosms.data.repositories.LicenseRepository;
import com.majorware.echosms.data.repositories.SMSRepository;
import com.majorware.echosms.data.tools.SMSQuotaManager;
import com.majorware.echosms.ui.home.HomeViewModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour l'envoi de SMS en arrière-plan
 */
public class SMSService extends Service implements SMSListener, OnLicenseValidationListener {

    private static final String TAG = "SMSService";
    private String collectionName;
    private String licenceKey;
    private SMSRepository smsRepository;
    private LicenseRepository licenseRepository;
    private Handler handler;
    private SMSQuotaManager quotaManager;
    private boolean isBatchProcessing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        licenseRepository = new LicenseRepository();
        handler = new Handler(getMainLooper());
        quotaManager = new SMSQuotaManager(this);
        SharedPreferences preferences = getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        collectionName = preferences.getString("firebaseCollectionName", "");
        licenceKey = preferences.getString("licenseKey","");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        checkAndSendPendingSMS();
        licenseRepository.validateLicense(licenceKey,getApplicationContext(),this);
        SMSRepository.getInstance().listenToSMSUpdates(this,collectionName); // Reconnecter Firestore en cas de redémarrage
        return START_STICKY;
    }

    public SMSService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("ForegroundServiceType")
    private void createNotification() {
        String channelId = "SMSServiceChannel";

        // Vérifie si la version Android est >= 26 (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "SMS Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Intent pour ouvrir l'Activity principale lorsque l'utilisateur clique sur la notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construire la notification avec l'intent attaché
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getApplication().getResources().getString(R.string.app_in_background_title))
                .setContentText(getApplication().getResources().getString(R.string.app_in_background_content))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)  // Ajout du PendingIntent ici
                .setAutoCancel(true) // Ferme la notification après le clic
                .build();

        // Lancer le service en mode foreground avec la notification
        startForeground(1, notification);
    }

    private void checkAndSendPendingSMS() {
        if (isBatchProcessing) {
            Log.d(TAG, "Batch en cours, ignoré");
            return;
        }

            SMSRepository.getInstance().getAllSMS(collectionName,
                smsList -> {
                    List<SMS> pendingSMS = smsList.stream()
                            .filter(sms -> "Pending".equalsIgnoreCase(sms.getStatus()))
                            .collect(Collectors.toList());

                    if (pendingSMS.isEmpty()) {
                        Log.d(TAG, "Aucun SMS en attente");
                        return;
                    }

                    isBatchProcessing = true;
                    processBatch(pendingSMS);
                },
                e -> {
                    Log.e(TAG, "Erreur lors de la récupération des SMS", e);
                    isBatchProcessing = false;
                }
        );
    }

    private void processBatch(List<SMS> pendingSMS) {
        int batchSize = Math.min(pendingSMS.size(), quotaManager.getBatchSize());
        Log.d(TAG, "Début du batch avec " + batchSize + " SMS");

        for (int i = 0; i < batchSize; i++) {
            SMS sms = pendingSMS.get(i);
            if (!quotaManager.canSendSMS()) {
                Log.w(TAG, "Quota atteint, arrêt du batch");
                break;
            }

            String phoneNumber = sms.getPhoneNumber();
            String message = sms.getMessage();
            String SMS_ID = sms.getId();

            try {
                sendSMSWithTimeout(phoneNumber, message, SMS_ID);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'envoi du SMS " + (i + 1) + "/" + batchSize, e);
                smsRepository.updateSMSStatus(SMS_ID, "Failed");
            }
        }

        if (pendingSMS.size() > batchSize) {
            quotaManager.scheduleNextBatch();
        } else {
            isBatchProcessing = false;
        }
    }

    private void sendSMSWithTimeout(String phoneNumber, String message, String SMS_ID) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            Intent sentIntent = new Intent("SMS_SENT");
            sentIntent.putExtra("smsObject", SMS_ID);
            PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            BroadcastReceiver smsBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    quotaManager.cancelAllTimeouts();
                    if (getResultCode() == Activity.RESULT_OK) {
                            SMSRepository.getInstance().updateSMSStatus(SMS_ID, "Sent");
                    } else {
                        // Récupérer le nombre de retry depuis les préférences
                        SharedPreferences preferences = getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
                        int maxRetries = preferences.getInt("retried", 3);
                        int currentRetries = SMSRepository.getInstance().getRetryCount(SMS_ID);

                        if (currentRetries < maxRetries) {
                            // Planifier le retry
                            quotaManager.scheduleRetry(SMS_ID, () -> {
                                SMSRepository.getInstance().incrementRetryCount(SMS_ID);
                                sendSMSWithTimeout(phoneNumber, message, SMS_ID);
                            });
                        } else {
                            SMSRepository.getInstance().updateSMSStatus(SMS_ID, "Failed");
                            Log.w(TAG, "Maximum retry attempts reached for SMS: " + SMS_ID);
                        }
                    }
                    try {
                        unregisterReceiver(this);
                    } catch (IllegalArgumentException e) {
                        // Déjà désenregistré
                    }
                }
            };

            // Enregistrer le BroadcastReceiver avec timeout
            registerReceiver(smsBroadcastReceiver, new IntentFilter("SMS_SENT"));
            
            // Créer et planifier le timeout
            Runnable timeoutRunnable = () -> {
                Log.w(TAG, "Timeout atteint pour SMS: " + SMS_ID);
                SMSRepository.getInstance().updateSMSStatus(SMS_ID, "Failed");
                try {
                    unregisterReceiver(smsBroadcastReceiver);
                } catch (IllegalArgumentException e) {
                    // Déjà désenregistré
                }
            };
            
            quotaManager.scheduleTimeout(timeoutRunnable);

            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null);
            SMSRepository.getInstance().updateSMSStatus(SMS_ID, "Processing");
            Log.d(TAG, "Envoie du SMS en cours pour: " + SMS_ID);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS", e);
            SMSRepository.getInstance().updateSMSStatus(SMS_ID, "Failed");
            quotaManager.cancelAllTimeouts();
        }
    }

    private boolean isValidPhoneNumber(String number) {
        // Accepte les numéros de type : +1234567890 ou 0612345678
        return number.matches("^\\+?[0-9]{8,15}$");
    }

    public static String cleanMessage(String message) {
        if (message == null) return "";

        // Supprimer le BOM
        message = message.replace("\uFEFF", "");

        // Supprimer les caractères invisibles unicode souvent problématiques
        message = message.replaceAll("[\u200B-\u200F\u202A-\u202E\u2060-\u206F]", "");

        // Supprimer les caractères de contrôle sauf \n et \t
        message = message.replaceAll("[\\p{C}&&[^\n\t]]", "");

        // Convertir les caractères spéciaux en espaces simples
        message = message.replaceAll("[\\r\\n]+", " ");  // remplace \r et \n par un espace
        message = message.replaceAll("[^\\p{Print}]", ""); // tout ce qui n’est pas imprimable

        // Nettoyer les espaces doubles ou multiples
        message = message.replaceAll("\\s{2,}", " ");

        // Trim final
        return message.trim();
    }

    @Override
    public void onSMSAdded() {
        this.checkAndSendPendingSMS();
    }

    @Override
    public void onSMSModified() {
        licenseRepository.validateLicense(licenceKey,getApplicationContext(),this);
    }

    @Override
    public void onValidationSuccess(boolean isValid) {

    }

    public void onValidationFailed(String errorMessage) {
        SharedPreferences prefs = getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLaunched", false);

        SMSRepository.getInstance().updateLaunchStatus(false); // ✅ Appel direct

        editor.apply();
        stopSelf();
    }
}