package com.majorware.echosms.ui.home;

import static android.content.ContentValues.TAG;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.majorware.echosms.data.models.SMS;
import com.majorware.echosms.data.models.ServicesAttribution;
import com.majorware.echosms.data.repositories.SMSRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class HomeViewModel extends AndroidViewModel {


    MutableLiveData<ServicesAttribution> servicesAttribution = new MutableLiveData<>();
    // Vous pouvez passer le contexte via un constructeur ou une méthode
    public HomeViewModel(@NonNull Application application) {
        super(application);

        servicesAttribution = SMSRepository.getInstance().getServicesAttribution();
    }



    public void setServiceState(){
        SharedPreferences preferences =  getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        servicesAttribution.postValue(new ServicesAttribution(
                preferences.getBoolean("isLaunched", false),
                preferences.getString("firebaseCollectionName", ""),
                preferences.getString("licenseKey",""),
                preferences.getInt("retried", 1)
        ));

    }

    public void updateLaunchStatus(boolean isLaunch) {
        SMSRepository.getInstance().updateLaunchStatus(isLaunch);
    }


    public void toggleService( boolean isLaunched ){
        SharedPreferences preferences = getApplication().getSharedPreferences("EchoSMS_Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLaunched", isLaunched);
        editor.apply();
        this.setServiceState();
    }

    /**
     *
     * @param pieChart
     */
    public void updateRingChart(PieChart pieChart) {
        SMSRepository.getInstance().getAllSMS(Objects.requireNonNull(servicesAttribution.getValue()).getFireCollectionSet(), smsList -> {
            // Calculer les entrées pour le graphique
            List<PieEntry> entries = calculateSMSStatuses(smsList);

            // Mettre à jour le graphique avec les nouvelles entrées
            ringChart(entries, pieChart);
        }, e -> {
            // Gérer l'erreur si la récupération des SMS échoue
            Log.e(TAG, "Error getting SMS data", e);
        });
    }
    private List<PieEntry> calculateSMSStatuses(List<SMS> smsList) {
        int sentCount = 0;
        int pendingCount = 0;
        int failedCount = 0;
        int processingCount = 0;

        // Compter le nombre de SMS pour chaque statut
        for (SMS sms : smsList) {
            switch (sms.getStatus()) {
                case "Sent":
                    sentCount++;
                    break;
                case "Pending":
                    pendingCount++;
                    break;
                case "Failed":
                    failedCount++;
                    break;
                case "Processing":
                    processingCount++;
                    break;
            }
        }

        // Calculer les pourcentages
        int totalCount = smsList.size();
        float sentPercentage = totalCount > 0 ? (sentCount / (float) totalCount) * 100 : 0;
        float pendingPercentage = totalCount > 0 ? (pendingCount / (float) totalCount) * 100 : 0;
        float failedPercentage = totalCount > 0 ? (failedCount / (float) totalCount) * 100 : 0;
        float processingPercentage = totalCount > 0 ? (processingCount / (float) totalCount) * 100 : 0;

        // Créer la liste des entrées pour le graphique
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(sentPercentage, "Sent"));
        entries.add(new PieEntry(pendingPercentage, "Pending"));
        entries.add(new PieEntry(failedPercentage, "Failed"));
        entries.add(new PieEntry(processingPercentage, "Processing"));

        return entries;
    }
    private void ringChart(List<PieEntry> entries, PieChart pieChart) {

        // Configuration du dataset
        PieDataSet dataSet = new PieDataSet(entries, "Messages");
        dataSet.setColors(Color.BLUE, Color.GREEN, Color.RED, Color.GRAY); // Couleurs personnalisées
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(3f); // Espacement entre les parts
        dataSet.setValueTextColor(Color.WHITE);

        // Configuration des données
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // Configuration du PieChart
        pieChart.setDrawHoleEnabled(true); // Active le trou central
        pieChart.setHoleRadius(50f); // Taille du trou
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Messages"); // Texte au centre
        pieChart.setCenterTextSize(16f);
        pieChart.setUsePercentValues(true); // Affichage en pourcentage
        pieChart.getDescription().setEnabled(false); // Désactiver la description
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);

        // Configuration de la légende
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);

        // Rafraîchir le graphique
        pieChart.invalidate();
    }

    /**
     *
     * @param lineChart
     */
    public void updateLineChart(LineChart lineChart) {

        SMSRepository.getInstance().getAllSMS(Objects.requireNonNull(servicesAttribution.getValue()).getFireCollectionSet(),smsList -> {
            // Calculer les données par heure pour les SMS envoyés dans les 24 dernières heures
            List<Entry> entries = calculateSMSByHour(smsList);

            // Mettre à jour le graphique avec les nouvelles entrées
            lineChart(entries, lineChart);
        }, e -> {
            // Gérer l'erreur si la récupération des SMS échoue
            Log.e(TAG, "Error getting SMS data", e);
        });
    }
    private List<Entry> calculateSMSByHour(List<SMS> smsList) {
        List<Entry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        long twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000); // 24 heures en millisecondes

        // Créez un tableau pour enregistrer le nombre de messages envoyés par heure (24 entrées possibles)
        int[] hourlyCounts = new int[24];

        // Parcours des SMS pour les regrouper par heure
        for (SMS sms : smsList) {
            if (sms.getCreatedAt() != null) {
                long smsTimestamp = sms.getCreatedAt().getSeconds() * 1000; // Convertir les secondes en millisecondes
                // Si le SMS a été envoyé dans les dernières 24 heures
                if (smsTimestamp >= twentyFourHoursAgo && smsTimestamp <= currentTime) {
                    int hour = getHourFromTimestamp(smsTimestamp); // Récupérer l'heure du SMS (de 0 à 23)
                    hourlyCounts[hour]++;
                }
            }

        }

        // Remplir les entrées avec les comptages par heure
        for (int i = 0; i < 24; i++) {
            entries.add(new Entry(i, hourlyCounts[i])); // Chaque point pour une heure
        }

        return entries;
    }
    // Méthode pour extraire l'heure d'un timestamp (de 0 à 23)
    private int getHourFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
    private void lineChart(List<Entry> entries, LineChart lineChart) {

        // Configuration du DataSet
        LineDataSet dataSet = new LineDataSet(entries, "Messages envoyés");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(12f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Ajouter les données
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);

        lineChart.setData(lineData);

        // Configuration du graphique
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        // Configuration des axes
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextSize(12f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);

        // Désactivation de l'axe Y droit
        lineChart.getAxisRight().setEnabled(false);

        // Configuration de la légende
        Legend legend = lineChart.getLegend();
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.LINE);

        // Rafraîchir le graphique
        lineChart.invalidate();
    }
}
