package com.majorware.echosms.ui.setting;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.majorware.echosms.R;
import com.majorware.echosms.data.models.ServicesAttribution;
import com.majorware.echosms.data.repositories.LicenseRepository;
import com.majorware.echosms.databinding.FragmentSettingBinding;
import com.majorware.echosms.ui.home.HomeViewModel;
import com.majorware.echosms.ui.info.SubscriptionInfoFragment;

public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;
    private  SettingViewModel settingViewModel;
    private static final char MASK_CHAR = '-';
    private boolean isFormatting;


    public SettingFragment() {
        this.isFormatting = false;
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
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingViewModel.loadSavedPreferences();
        setUpCheckedChangeListener();
        setTextChangedListenerForLicenseKey();
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.toast_loading_verification)); // Ajoute cette string à tes ressources
        progressDialog.setCancelable(false);
        binding.btnSeeMore.setOnClickListener(v->{
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SubscriptionInfoFragment subscriptionInfoFragmentFragment = new SubscriptionInfoFragment();
            fragmentTransaction.replace(R.id.fragmentContainerView, subscriptionInfoFragmentFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
        settingViewModel.servicesAttribution.observe(getViewLifecycleOwner(), servicesAttribution -> {
            setPreferences(servicesAttribution);
            loadInfoLicense(servicesAttribution);
        });

        settingViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                progressDialog.show();
            } else {
                progressDialog.dismiss();
            }
        });

        settingViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        settingViewModel.getAlertError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.dialog_title_validation_failed))
                        .setMessage(error)
                        .setCancelable(true)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        settingViewModel.getLicenseValid().observe(getViewLifecycleOwner(), isValid -> {
            if (isValid != null && isValid) {
                settingViewModel.loadSavedPreferences();
            }
        });


    }

    private void setTextChangedListenerForLicenseKey(){
        binding.etLicenseRenewal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;  // Évite une boucle infinie

                isFormatting = true;

                // Convertir en majuscules et enlever les tirets existants
                String text = s.toString().toUpperCase().replace(String.valueOf(MASK_CHAR), "");
                StringBuilder formattedText = new StringBuilder();
                int cursorPosition = binding.etLicenseRenewal.getSelectionStart();
                int originalLength = s.length();

                // Ajouter les tirets au bon endroit
                for (int i = 0; i < text.length() && i < 20; i++) {  // Limite à 20 caractères (hors tirets)
                    if (i > 0 && i % 4 == 0) {
                        formattedText.append(MASK_CHAR);
                    }
                    formattedText.append(text.charAt(i));
                }
                // Vérifie si le texte est déjà formaté
                if (!s.toString().equals(formattedText.toString())) {
                    binding.etLicenseRenewal.removeTextChangedListener(this);
                    binding.etLicenseRenewal.setText(formattedText.toString());

                    // Calcul de la nouvelle position du curseur
                    int addedChars = formattedText.length() - originalLength;
                    int newCursorPos = cursorPosition + addedChars;
                    // Assurons-nous que la position est valide
                    newCursorPos = Math.max(0, Math.min(newCursorPos, formattedText.length()));
                    binding.etLicenseRenewal.setSelection(newCursorPos);

                    binding.etLicenseRenewal.addTextChangedListener(this);
                }

                isFormatting = false;
            }
        });
    }

    private void setUpCheckedChangeListener() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            SharedPreferences preferences = requireActivity().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            if (buttonView.getId() == R.id.cb_firebase_changes) {
                binding.etFirebaseCollection.setEnabled(isChecked);
                editor.putString("firebaseCollectionName", binding.etFirebaseCollection.getText().toString());
            } else if(buttonView.getId() == R.id.cb_renewal_license){
                binding.etLicenseRenewal.setEnabled(isChecked);
                settingViewModel.verifyLicence(binding.etLicenseRenewal.getText().toString(),requireContext());
            }
            else if(buttonView.getId() == R.id.cb_retry_changes){
                    binding.etRetryCount.setEnabled(isChecked);
                    editor.putInt("retried", Integer.parseInt(binding.etRetryCount.getText().toString()));
            }

            editor.apply();
        };

        // Attacher le listener aux deux composants
        binding.cbFirebaseChanges.setOnCheckedChangeListener(listener);
        binding.cbRetryChanges.setOnCheckedChangeListener(listener);
        binding.cbRenewalLicense.setOnCheckedChangeListener(listener);
    }

    private void setPreferences(ServicesAttribution servicesAttribution){
        binding.etFirebaseCollection.setText(servicesAttribution.getFireCollectionSet());
        binding.etLicenseRenewal.setText(servicesAttribution.getLicenseKey());
        binding.etRetryCount.setText(String.valueOf(servicesAttribution.getSendingAttempts()));
    }
    private void loadInfoLicense(ServicesAttribution servicesAttribution){
        LicenseRepository licenseRepository = new LicenseRepository();
        licenseRepository.getCurrentLicense(  license -> {
                    binding.tvPlanValue.setText(license.getPlan());
                    binding.tvBenefits.setText(license.getBenefits());
                    binding.tvSubscriptionTime.setText(license.getSubscriptionDelay());

                },
                e -> Log.w("Setting", "Error while retrieving license key", e), servicesAttribution.getLicenseKey()
        );
    }
}