package com.majorware.echosms.ui.home;

import static android.Manifest.permission.SEND_SMS;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.majorware.echosms.R;
import com.majorware.echosms.data.interfaces.OnLicenseValidationListener;
import com.majorware.echosms.data.interfaces.SMSListCallback;
import com.majorware.echosms.data.interfaces.SMSListener;
import com.majorware.echosms.data.models.SMS;
import com.majorware.echosms.data.models.SMSLogAdapter;
import com.majorware.echosms.data.models.ServicesAttribution;
import com.majorware.echosms.data.repositories.FirebaseRepository;
import com.majorware.echosms.data.repositories.LicenseRepository;
import com.majorware.echosms.data.repositories.SMSRepository;
import com.majorware.echosms.data.services.SMSService;
import com.majorware.echosms.databinding.FragmentHomeBinding;
import com.majorware.echosms.ui.setting.SettingFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends Fragment implements SMSListener, OnLicenseValidationListener {
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;
    private FragmentHomeBinding binding;
    private  HomeViewModel homeViewModel;
    private   SMSRepository smsRepository;
    private SMSLogAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // Inflate the layout for this fragment
        homeViewModel.setServiceState();
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LicenseRepository licenseRepository = new LicenseRepository();
 setupRecyclerView();

        homeViewModel.servicesAttribution.observe(getViewLifecycleOwner(), servicesAttribution -> {
             if (servicesAttribution == null) return;
            licenseRepository.validateLicense(servicesAttribution.getLicenseKey(),requireContext(),this);
            connectionIndicator(servicesAttribution);
            updateServiceState(servicesAttribution);
            homeViewModel.updateRingChart(binding.pieChart);
            homeViewModel.updateLineChart(binding.lineChart);
           
             if (servicesAttribution != null &&  servicesAttribution.getFireCollectionSet() != null && servicesAttribution.getFireCollectionSet().isEmpty()) {
                loadRecentSMSLogs(SMSRepository.getInstance(), servicesAttribution.getFireCollectionSet());
            }
        });
        binding.toggleServiceButton.setOnClickListener(v->{
               homeViewModel.toggleService(!Objects.requireNonNull(homeViewModel.servicesAttribution.getValue()).isLaunch());
        });
        binding.btnGoToSetting.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SettingFragment settingFragment = new SettingFragment();
            fragmentTransaction.replace(R.id.fragmentContainerView, settingFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
        
        

    }


    private boolean isServiceRunning() {
        try {
            ActivityManager manager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (SMSService.class.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error while verifying the service : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    @SuppressLint("ObsoleteSdkInt")
    private void updateServiceState(ServicesAttribution servicesAttribution) {
        FirebaseRepository firebaseRepository = new FirebaseRepository();

        try {
            if (servicesAttribution.isLaunch()) {

                binding.toggleServiceButton.setText(getResources().getString(R.string.button_text_stop_service));
                binding.toggleServiceButton.setBackgroundColor(getResources().getColor(R.color.red));

                if (servicesAttribution.getFireCollectionSet() == null || servicesAttribution.getFireCollectionSet().isEmpty()) {
                    Toast.makeText(requireContext(), "Aucune collection Firebase définie", Toast.LENGTH_SHORT).show();
                    return;
                }

                SMSRepository.getInstance().listenToSMSUpdates(this, servicesAttribution.getFireCollectionSet());

                // Vérifier si l'application a la permission d'envoyer des SMS
                if (ContextCompat.checkSelfPermission(requireContext(), SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
                    return;
                }

                Intent intent = new Intent(requireContext(), SMSService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(requireContext(), intent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    requireContext().startService(intent);
                } else {
                    Toast.makeText(requireContext(), "Version Android not supported for starting the service", Toast.LENGTH_LONG).show();
                }

            } else {
                binding.toggleServiceButton.setText(getResources().getString(R.string.button_text_start_service));
                binding.toggleServiceButton.setBackgroundColor(getResources().getColor(R.color.green));
                firebaseRepository.stopObservingFirestore();

                if (isServiceRunning()) {
                    requireContext().stopService(new Intent(requireContext(), SMSService.class));
                }
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error occured while Starting/Stopping the service : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("SMSService", "updateServiceState error", e);
        }
    }


    private void connectionIndicator(ServicesAttribution servicesAttribution) {
        FirebaseRepository firebaseRepository = new FirebaseRepository();
        firebaseRepository.observeFirestoreConnection(requireContext(), (querySnapshot, error) -> {
            if (error != null) {
                // 🔴 Firestore n'est pas accessible
                binding.serviceStatusIndicator.setIndicatorColor(getResources().getColor(R.color.red));
                binding.serviceStatusText.setText(getResources().getString(R.string.service_status_offline));

                return;
            }

            if ((querySnapshot != null) && !Objects.requireNonNull(querySnapshot.getDocuments()).isEmpty()) {
                if(servicesAttribution.isLaunch()) {
                    // ✅ Firestore est accessible (au moins un document trouvé)
                    binding.serviceStatusIndicator.setIndicatorColor(getResources().getColor(R.color.green));
                    binding.serviceStatusText.setText(getResources().getString(R.string.service_status_online));
                    binding.serviceStatusText.setTextColor(getResources().getColor(R.color.green));
                }else{
                    binding.serviceStatusIndicator.setIndicatorColor(getResources().getColor(R.color.orange));
                    binding.serviceStatusText.setText(getResources().getString(R.string.service_status_pause));
                    binding.serviceStatusText.setTextColor(getResources().getColor(R.color.orange));
                }
            } else {
                // 🔴 Firestore inaccessible (aucun document trouvé)
                binding.serviceStatusIndicator.setIndicatorColor(getResources().getColor(R.color.red));
                binding.serviceStatusText.setText(getResources().getString(R.string.service_status_offline));
                binding.serviceStatusText.setTextColor(getResources().getColor(R.color.red));
            }

        }, servicesAttribution.getLicenseKey());


    }
    private void setupRecyclerView() {
        binding.smsLogsRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SMSLogAdapter(new ArrayList<>());
        binding.smsLogsRecyclerview.setAdapter(adapter);
    }
    private void loadRecentSMSLogs(SMSRepository smsRepository, String collectionName) {
    if (smsRepository == null || collectionName == null) {
        Log.e("HomeFragment", "SMSRepository or collectionName is null");
        return;
    }
    smsRepository.getRecentSMSLogs(smsList -> {
        if (adapter == null) {
            adapter = new SMSLogAdapter(smsList);
            binding.smsLogsRecyclerview.setAdapter(adapter);
        } else {
            adapter.updateData(smsList);
        }
    }, collectionName);
}

    private void showLicenseErrorAndExit() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getResources().getString(R.string.dialog_title_expiring))
                .setMessage(getResources().getString(R.string.dialog_msg_renew_license))
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, which) -> { dialog.dismiss();}) // Ferme complètement l'application
                .show();

    }

    @Override
    public void onSMSAdded() {
        updateUI();
         }

    @Override
    public void onSMSModified() {
     updateUI();
    }

    private void updateUI(){
        homeViewModel.updateRingChart(binding.pieChart);
        homeViewModel.updateLineChart(binding.lineChart);

    }

    @Override
    public void onValidationSuccess(boolean isValid) {
        if(isValid){
            homeViewModel.setServiceState();
        }

    }

    @Override
    public void onValidationFailed(String errorMessage) {
        showLicenseErrorAndExit();
    }
}