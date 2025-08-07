package com.majorware.echosms.ui.setting;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.majorware.echosms.R;
import com.majorware.echosms.data.interfaces.OnLicenseValidationListener;
import com.majorware.echosms.data.models.ServicesAttribution;
import com.majorware.echosms.data.repositories.LicenseRepository;

public class SettingViewModel extends AndroidViewModel {
    public SettingViewModel(@NonNull Application application) {
        super(application);
    }
    MutableLiveData<ServicesAttribution> servicesAttribution = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> alertError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> licenseValid = new MutableLiveData<>();
    public void loadSavedPreferences() {
        SharedPreferences preferences = getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        servicesAttribution.postValue(new ServicesAttribution(
                preferences.getBoolean("isLaunched", false),
                preferences.getString("firebaseCollectionName", ""),
                preferences.getString("licenseKey",""),
                preferences.getInt("retried", 1)

        ));

    }


    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<String> getAlertError() {
        return alertError;
    }

    public LiveData<Boolean> getLicenseValid() {
        return licenseValid;
    }

    public void verifyLicence(String licenseKey, Context appContext) {
        LicenseRepository licenseRepository = new LicenseRepository();

        if (licenseKey.length() != 24 || !isValidFormat(licenseKey)) {
            toastMessage.postValue(appContext.getString(R.string.toast_add_valid_licence_key));
            return;
        }

        isLoading.postValue(true);

        try {
            licenseRepository.validateLicense(licenseKey, appContext, new OnLicenseValidationListener() {
                @Override
                public void onValidationSuccess(boolean isValid) {
                    isLoading.postValue(false);

                    if (isValid) {
                        toastMessage.postValue(appContext.getString(R.string.toast_validate_licence));

                        SharedPreferences preferences = appContext.getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("licenseKey", licenseKey);
                        editor.apply();

                        licenseValid.postValue(true);

                    } else {
                        toastMessage.postValue(appContext.getString(R.string.toast_licence_not_validate));
                        licenseValid.postValue(false);
                    }
                }

                @Override
                public void onValidationFailed(String errorMessage) {
                    isLoading.postValue(false);
                    alertError.postValue(appContext.getString(R.string.dialog_msg_error) + " " + errorMessage);
                }
            });

        } catch (Exception e) {
            isLoading.postValue(false);
            toastMessage.postValue("Erreur: " + e.getMessage());
        }
    }


    /**
     * Vérifie si le format de la licence est valide
     */
    private boolean isValidFormat(String licenseKey) {
        // Format attendu: XXXX-XXXX-XXXX-XXXX-XXXX
        return licenseKey.matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");
    }
}
