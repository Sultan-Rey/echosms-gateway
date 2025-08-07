package com.majorware.echosms.ui.info;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.Timestamp;
import com.majorware.echosms.R;
import com.majorware.echosms.data.repositories.LicenseRepository;
import com.majorware.echosms.data.repositories.SMSRepository;
import com.majorware.echosms.databinding.FragmentSubscriptionInfoBinding;
import com.majorware.echosms.data.models.SMS;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;



public class SubscriptionInfoFragment extends Fragment {

    private FragmentSubscriptionInfoBinding binding;
    public SubscriptionInfoFragment() {
        // Required empty public constructor
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding =  FragmentSubscriptionInfoBinding.inflate(inflater, container, false);
        return  binding.getRoot();
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Création et affichage du ProgressDialog
        LicenseRepository licenseRepository = new LicenseRepository();
        SharedPreferences preferences = requireActivity().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        String licenseKey = preferences.getString("licenseKey", "");
        if(!licenseKey.isEmpty()) {
            licenseRepository.getCurrentLicense(
                    license -> {
                        String state = license.isActive() ? license.getLicenceKey() : license.getLicenceKey() + " Expired";
                        binding.tvPlan.setText(license.getPlan());
                        binding.tvBenefits.setText(license.getBenefits());
                        binding.tvDeviceId.setText(license.getDeviceId());

                        // ✅ Vérifie si expiryDate est null
                        if (license.getExpiryDate() != null) {
                            binding.tvExpiryDate.setText(license.getExpiryDate().toDate().toString());
                        } else {
                            binding.tvExpiryDate.setText("Unavailable");
                        }

                        binding.tvLicenseKey.setText(license.getLicenceKey());
                        if(!license.isActive()){
                            binding.tvStatus.setTextColor(getResources().getColor(R.color.red));
                        }
                        binding.tvStatus.setText(state);
                        binding.tvSubscriptionDelay.setText(license.getSubscriptionDelay());

                        // ✅ Vérifie si purchaseDate est null
                        if (license.getPurchaseDate() != null) {
                            binding.tvPurchaseDate.setText(license.getPurchaseDate().toDate().toString());

                            calculateExceedFees(license.getPurchaseDate(), fees -> {
                                binding.tvExcessCosts.setText(fees + " $");
                                binding.tvRenewalCosts.setText((fees + license.getCost()) + " $");
                            });
                        } else {
                            binding.tvPurchaseDate.setText("Unavailable");
                            binding.tvExcessCosts.setText("N/A");
                            binding.tvRenewalCosts.setText("N/A");
                        }

                    },
                    e -> Log.d("Firestore", "Erreur lors de la récupération de la licence", e),
                    licenseKey
            );

        }



    }


    private void calculateExceedFees(Timestamp purchaseTimestamp, OnFeesCalculatedListener listener) {
        SMSRepository smsRepository = new SMSRepository();
        SharedPreferences preferences = requireActivity().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        String collectionName = preferences.getString("firebaseCollectionName", "");
        smsRepository.getAllSMS(collectionName,
                smsList -> {
            // Dictionnaire pour stocker le nombre de SMS envoyés par mois
            Map<Long, Integer> monthlySMSCount = new HashMap<>();

            for (SMS sms : smsList) {
                if (sms.getStatus().equalsIgnoreCase("Sent")) {
                    // Convertir le Timestamp en millisecondes
                    Timestamp smsTimestamp = sms.getCreatedAt();
                    long smsMillis = smsTimestamp.getSeconds() * 1000 + smsTimestamp.getNanoseconds() / 1000000; // Conversion en millisecondes

                    // Calculer le début du mois pour ce SMS
                    long monthStart = getMonthStart(smsMillis);

                    // Incrémenter le nombre de SMS pour ce mois
                    monthlySMSCount.put(monthStart, monthlySMSCount.getOrDefault(monthStart, 0) + 1); //Succeptible de gerer nullpointer, comment arranger cela
                }
            }

            double totalFees = 0.0;

            // Convertir purchaseTimestamp en millisecondes pour obtenir le début du mois
            long purchaseMillis = purchaseTimestamp.getSeconds() * 1000 + purchaseTimestamp.getNanoseconds() / 1000000;
            long purchaseMonthStart = getMonthStart(purchaseMillis);

            for (Map.Entry<Long, Integer> entry : monthlySMSCount.entrySet()) {
                long monthStart = entry.getKey();
                int sentCount = entry.getValue();

                // Ne compter que les mois après ou égaux à la date d'achat
                if (monthStart >= purchaseMonthStart) {
                    int smsExceeding = Math.max(sentCount - 1000, 0); // 1000 gratuits chaque mois
                    totalFees += (smsExceeding / 1000.0) * 0.011; // 0.011$ par tranche de 1000 SMS excédentaires
                }
            }

            // Passer le résultat au callback
            listener.onFeesCalculated(totalFees);

        }, e -> {
            Log.e(TAG, "Error getting SMS data", e);
            listener.onFeesCalculated(0.0); // Retourne 0 en cas d'erreur
        });
    }

    // Fonction pour obtenir le début du mois en millisecondes
    private long getMonthStart(long timestamp) {
        return timestamp - (timestamp % TimeUnit.DAYS.toMillis(30)); // Approximation en ignorant les jours exacts
    }

    // Interface callback pour récupérer les frais calculés
    public interface OnFeesCalculatedListener {
        void onFeesCalculated(double fees);
    }






}