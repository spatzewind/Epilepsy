package com.metzner.enrico.epilepsy.diagrams;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.seizure.SeizureHolder;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.users.UserHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class PiechartsActivity extends AppCompatActivity {

    private static final String TAG = "PIECHARTS_ACTIVITY";

    private String[] userStrings;
    private String[] userList;
    private File[] userDirectory;
    private String selectedUserString;
    private String selectedUserDirectoryName;
    private int minDate;
    private ArrayList<SeizureHolder> seizureEntries;
    private PieChart pieChart;
    private ArrayList<String> pie_titles;
    private ArrayList<Float> pie_data;
    Spinner pieChart_dataType_spinner;
    private boolean typeSpinnerHasListener;




    /**********************************************************************************************
     **** CONFIGURATION ***************************************************************************
     **********************************************************************************************/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
        Log.d(TAG, "attachBaseContext");
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
        Log.d(TAG, "onConfigurationChanged: "+newConfig.locale.getLanguage());
    }




    /**********************************************************************************************
     **** INITIALISATION **************************************************************************
     **********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.resetTitle(this);
        setContentView(R.layout.activity_piecharts);

        if( !PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
        }

        pieChart = (PieChart) findViewById(R.id.pie_chart_pie_charts);
        pie_titles = new ArrayList<>();
        pie_data = new ArrayList<>();
        fillUserList();
//        loadSeizures();

        pieChart_dataType_spinner = (Spinner) findViewById(R.id.spinner_piecharts);
        pieChart_dataType_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: position = "+position);
                fillPieChart(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //return;
            }
        });
        typeSpinnerHasListener = true;
    }




    /**********************************************************************************************
     **** PIE-CHART *******************************************************************************
     **********************************************************************************************/
    private void fillUserList() {
        typeSpinnerHasListener = false;
        ArrayList<String> tempUsers = UserEntryHelper.loadUsers(this);
        boolean unassignedDataExist = SeizureEntryHelper.existNotAssignedData(this);
        int userCount = Math.max(1, tempUsers.size()+(unassignedDataExist?1:0));
        userList = new String[userCount];
        userStrings = new String[userCount];
        userDirectory = new File[userCount];
        for(int u=0; u<userCount; u++) {
            if(u<tempUsers.size()) {
                UserHolder tempUser = new UserHolder(this, tempUsers.get(u));
                userList[u] = tempUser.getName();
                userDirectory[u] = tempUser.getUserDir();
                userStrings[u] = tempUser.getUserEntryString();
            } else {
                userList[u] = getResources().getString(R.string.unknown_user);
                userDirectory[u] = null;
                userStrings[u] = UserEntryHelper.UserEntryFillString;
            }
        }
        int selectedUser = (unassignedDataExist ? userList.length-1 : 0);

        Spinner userSpinner = (Spinner) findViewById(R.id.spinner_pie_chart_users);
        ArrayAdapter<String> userAdapter = new ArrayAdapter<>(this, R.layout.user_spinner_item, userList);
        userAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        userSpinner.setAdapter(userAdapter);
        userSpinner.setSelection(selectedUser);
        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(userList[position].equals(getResources().getString(R.string.unknown_user))) {
                    selectedUserString = UserEntryHelper.UserEntryFillString;
                    selectedUserDirectoryName = null;
                } else {
                    selectedUserString = userStrings[position];
                    selectedUserDirectoryName = userDirectory[position].getName();
                }
                loadSeizures();
                if(typeSpinnerHasListener) fillPieChart(pieChart_dataType_spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing happens
            }
        });
    }
    private void loadSeizures() {
        Log.d(TAG, "loadSeizures ...");
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        ArrayList<String> seizures = new ArrayList<>();
        seizures.clear();
        if(PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            for (int m = 12; m > 8; m--) {
                int month2 = (month + m) % 12;
                int year2 = (month2 > month ? year - 1 : year);
                Log.d(TAG, "Load data from <" + year2 + "|" + month2 + ">-file ...");
                seizures.addAll(SeizureEntryHelper.loadSeizureList(this, selectedUserDirectoryName, year2, month2));
            }
        }
        seizureEntries = new ArrayList<>();
        seizureEntries.clear();
        for(String entry: seizures) {
            seizureEntries.add(new SeizureHolder(this, entry));
        }
        long minTime = System.currentTimeMillis() - 8640000000L;
        calendar.setTimeInMillis(minTime);
        minDate = calendar.get(Calendar.YEAR)*10000 + calendar.get(Calendar.MONTH)*100 + calendar.get(Calendar.DAY_OF_MONTH);
        Log.d(TAG, "loadSeizures: "+seizureEntries.size()+" entries");
        Log.d(TAG, "loadSeizures: minDate = "+minDate);
    }
    private void loadSeizureType() {
        Log.d(TAG, "loadSeizureType ...");
        pie_titles.clear();
        pie_titles.addAll(Arrays.asList(getResources().getStringArray(R.array.seizure_type_list)));

        int[] newData = new int[pie_titles.size()];
        for(SeizureHolder sh: seizureEntries) {
            int[] date = sh.getSeizureDate();
            int shDate = date[0]*10000 + date[1]*100 + date[2];
            if(shDate>minDate) {
                newData[sh.getSeizureTypeID()]++;
            }
        }
        pie_data.clear();
        for(int d=0; d<newData.length; d++) {
            Log.d(TAG, "loadSeizureType: pie_data["+d+"]="+newData[d]+" -> "+pie_titles.get(d));
            pie_data.add((float) newData[d]);
        }
    }
    private void loadWarnings() {
        pie_titles.clear();
        pie_titles.addAll(Arrays.asList(getResources().getStringArray(R.array.ns_warning_list)));
        int[] newData = new int[pie_titles.size()];
        for(SeizureHolder sh: seizureEntries) {
            int[] date = sh.getSeizureDate();
            int shDate = date[0]*10000 + date[1]*100 + date[2];
            if(shDate>minDate) {
                String w = sh.getWarnings(this, true);
                String[] wIDs = w.split("#");
                for(String w_id: wIDs) {
                    newData[Integer.parseInt(w_id)]++;
                }
            }
        }
        pie_data.clear();
        for(int d=0; d<newData.length; d++) {
            Log.d(TAG, "loadWarnings: pie_data["+d+"]="+newData[d]+" -> "+pie_titles.get(d));
            pie_data.add((float) newData[d]);
        }
    }
    private void loadTrigger() {
        pie_titles.clear();
        pie_titles.addAll(Arrays.asList(getResources().getStringArray(R.array.ns_trigger_list)));
        int[] newData = new int[pie_titles.size()];
        for(SeizureHolder sh: seizureEntries) {
            int[] date = sh.getSeizureDate();
            int shDate = date[0]*10000 + date[1]*100 + date[2];
            if(shDate>minDate) {
                String w = sh.getTriggers(this, true);
                String[] wIDs = w.split("#");
                for(String w_id: wIDs) {
                    newData[Integer.parseInt(w_id)]++;
                }
            }
        }
        pie_data.clear();
        for(int d=0; d<newData.length; d++) {
            Log.d(TAG, "loadTrigger: pie_data["+d+"]="+newData[d]+" -> "+pie_titles.get(d));
            pie_data.add((float) newData[d]);
        }
    }
    private void loadEmergency() {
        pie_titles.clear();
        pie_titles.addAll(Arrays.asList(getResources().getStringArray(R.array.ns_emergency_list)));
        int[] newData = new int[pie_titles.size()];
        for(SeizureHolder sh: seizureEntries) {
            int[] date = sh.getSeizureDate();
            int shDate = date[0]*10000 + date[1]*100 + date[2];
            if(shDate>minDate) {
                newData[sh.getSeizureEmergency()]++;
            }
        }
        pie_data.clear();
        for (int d=0; d<newData.length; d++) {
            Log.d(TAG, "loadEmergency: pie_data["+d+"]="+newData[d]+" -> "+pie_titles.get(d));
            pie_data.add((float) newData[d]);
        }
    }
    private void loadDaytime() {
        pie_titles.clear();
        pie_titles.addAll(Arrays.asList(getResources().getStringArray(R.array.ns_daytime_list)));
        int[] newData = new int[pie_titles.size()];
        for(SeizureHolder sh: seizureEntries) {
            int[] date = sh.getSeizureDate();
            int shDate = date[0]*10000 + date[1]*100 + date[2];
            if(shDate>minDate) {
                newData[sh.getSeizureDaytime()]++;
            }
        }
        pie_data.clear();
        for(int d=0; d<newData.length; d++) {
            Log.d(TAG, "loadDaytime: pie_data["+d+"]="+newData[d]+" -> "+pie_titles.get(d));
            pie_data.add((float) newData[d]);
        }
    }
    private void fillPieChart(int _spinnerPosition) {
        Log.d(TAG, "fillPieChart...");
        switch(_spinnerPosition) {
            case 0: loadSeizureType(); break; //seizure type
            case 1: loadWarnings(); break; //warnings
            case 2: loadTrigger(); break; //trigger
            case 3: loadEmergency(); break; //emergency
            case 4: loadDaytime(); break; //time of day
            default: //return;
        }

        pieChart.getDescription().setText("");
        pieChart.setRotationEnabled(true);
        pieChart.setHoleRadius(0);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setDrawEntryLabels(false);
//        pieChart.setEntryLabelColor(getResources().getColor(R.color.black));
//        pieChart.setEntryLabelTextSize(16);
//        pieChart.setUsePercentValues(true);

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
//        ArrayList<String> pieLabels = new ArrayList<>();
        pieEntries.clear();
//        pieLabels.clear();
        for(int e=0; e<pie_data.size(); e++) {
            float value = pie_data.get(e);
            if(value>0.1) {
                pieEntries.add(new PieEntry(value, pie_titles.get(e)));
                Log.d(TAG, "fillPieChart: pieEntry set ("+value+"|"+pie_titles.get(e)+")");
//                pieLabels.add(pie_titles.get(e));
            }
        }
        ArrayList<Integer> colours = new ArrayList<>();
        colours.clear();
        int[] colorArray = getResources().getIntArray(R.array.colors_pie_chart);
        if(pieEntries.size()>1) {
            for(int c=1; c<=pieEntries.size(); c++) {
                colours.add(colorArray[c%colorArray.length]);
            }
        } else {
//            pieEntries.add(new PieEntry(0.2f, " "));
            colours.add(colorArray[0]);
//            colours.add(colorArray[1]);
//            colours.add(getResources().getColor(R.color.light_violet));
        }
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setSliceSpace(2);
        pieDataSet.setValueTextSize(16);
        pieDataSet.setValueTextColor(getResources().getColor(R.color.white));
        pieDataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return ""+(value<0.1 ? "" : (int) value);
            }
        });
        pieDataSet.setColors(colours);
        pieChart.setData(new PieData(pieDataSet));

        Legend l = pieChart.getLegend();
        l.setWordWrapEnabled(true);
//        l.setOrientation(Legend.LegendOrientation.VERTICAL);

        pieChart.invalidate();
    }

}
