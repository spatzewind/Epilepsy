package com.metzner.enrico.epilepsy.diagrams;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.seizure.SeizureHolder;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.users.UserHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TimeSeriesActivity extends AppCompatActivity {

    private static final String TAG = "TIME_SERIES_ACTIVITY";

    private Locale currentLocale;
    private Context context;
    private boolean hasPermission;

    private String[] userStrings;
    private String[] userList;
    private File[] userDirectory;
    private String selectedUserString;
    private String selectedUserDirectoryName;
    private TextView choiceDateInfo;
    private SeekBar choiceDateSeekBar;
    private BarChart barChart;
    private String dateTemplate;
    private int[][] dates;
    private int[] choosenDate;
    private Spinner userSpinner, timeSeriesSpinner;
    private int timeSeriesMode;
    private int[] barChartValues;
    private String[] barChartLabels;
    private int entriesMaxCount;
    private boolean typeSpinnerHasListener;

    private static final int TS_YEAR =  1;
    private static final int TS_MONTH = 2;




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
        setContentView(R.layout.activity_time_series);
        currentLocale = getResources().getConfiguration().locale;
        context = this;
        hasPermission = PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(!hasPermission) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
        }

        choiceDateInfo = (TextView) findViewById(R.id.dia_ts_date);
        choiceDateSeekBar = (SeekBar) findViewById(R.id.dia_ts_date_seekbar);
        barChart = (BarChart) findViewById(R.id.bar_chart_time_series);

        fillUserList();

        timeSeriesSpinner = (Spinner) findViewById(R.id.spinner_time_series);
        timeSeriesSpinner.setOnItemSelectedListener(new setSeekBarRange());
        choiceDateSeekBar.setOnSeekBarChangeListener(new setTimeSeriesContent());
        if(Build.VERSION.SDK_INT>23) {
            choiceDateSeekBar.setTickMark(getDrawable(R.drawable.dia_bg_piechart_seekbar_tickmark));
        }
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        typeSpinnerHasListener = false;
    }

    /**********************************************************************************************
     **** SPINNER / SEEKBAR - INTERACTION *********************************************************
     **********************************************************************************************/
    private class setSeekBarRange implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Calendar calendar = Calendar.getInstance();
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            switch(position) {
                case 0: // year view
                    timeSeriesMode = TS_YEAR;
                    int minYearCount = Math.max(10, currentYear-2015);
                    dateTemplate = "yyyy";
                    dates = new int[minYearCount][2];
                    for(int d=0; d<minYearCount; d++) {
                        dates[d][0] = currentYear+d+1-minYearCount;
                        dates[d][1] = 0;
                    }
                    choiceDateSeekBar.setMax(minYearCount-1);
                    choiceDateSeekBar.setProgress(minYearCount-1);
                    break;
                case 1: // month view
                    timeSeriesMode = TS_MONTH;
                    int minMonthCount = Math.min(24, (currentYear-2016)*12+currentMonth);
                    dateTemplate = getResources().getString(R.string.diagram_ts_dateFormat);
                    dates = new int[minMonthCount][2];
                    for(int d=0; d<minMonthCount; d++) {
                        int m = (currentMonth+36-d)%12;
                        int y = currentYear-(d/12)-(m>currentMonth ? 1 : 0);
                        dates[minMonthCount-1-d][0] = y;
                        dates[minMonthCount-1-d][1] = m;
                    }
                    choiceDateSeekBar.setMax(minMonthCount-1);
                    choiceDateSeekBar.setProgress(minMonthCount-1);
                    break;
                default: //return;
            }
            choosenDate = new int[]{currentYear,currentMonth,15};
            getTimeSeriesData();
            fillBarChart();
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //return;
        }
    }
    private class setTimeSeriesContent implements OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            choosenDate = new int[]{dates[progress][0],dates[progress][1],15};
            choiceDateInfo.setText(
                    SeizureEntryHelper.date2string(choosenDate, dateTemplate, currentLocale, context)
            );
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //return;
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            getTimeSeriesData();
            fillBarChart();
        }
    }




    /**********************************************************************************************
     **** TIME-SERIES LINE-CHART ******************************************************************
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

        userSpinner = (Spinner) findViewById(R.id.spinner_time_series_users);
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
                if(typeSpinnerHasListener) {
//                    int possilbePosition = getResources().getStringArray(R.array.diagram_timeseries_list).length;
//                    Log.d(TAG, "onItemSelected: TIMESERIES-MODE has "+possilbePosition+" possible values");
//                    int ts_position = timeSeriesSpinner.getSelectedItemPosition();
//                    timeSeriesSpinner.setSelection((ts_position+1)%possilbePosition);
//                    Log.d(TAG, "onItemSelected: change selection of TIMESERIES-MODE to "+(timeSeriesMode==TS_YEAR ? "TS_YEAR" : "TS_MONTH"));
//                    Log.d(TAG, "onItemSelected: should changed to "+(ts_position+2==TS_YEAR ? "TS_YEAR" : "TS_MONTH"));
//                    timeSeriesSpinner.setSelection(ts_position);
//                    Log.d(TAG, "onItemSelected: change selection of TIMESERIES-MODE to "+(timeSeriesMode==TS_YEAR ? "TS_YEAR" : "TS_MONTH"));
//                    Log.d(TAG, "onItemSelected: should changed to "+(ts_position+1==TS_YEAR ? "TS_YEAR" : "TS_MONTH"));
                    getTimeSeriesData();
                    Log.d(TAG, "onItemSelected:   run <getTimeSeriesData()>");
                    fillBarChart();
                    Log.d(TAG, "onItemSelected:   run <fillBarChart()>");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing happens
            }
        });
    }
    private void getTimeSeriesData() {
        switch (timeSeriesMode) {
            case TS_YEAR: // year view
                barChartLabels = null;
                barChartLabels = getResources().getStringArray(R.array.monthAbbr);
                barChartValues = new int[12];
                entriesMaxCount = 0;
                if(hasPermission) {
                    for (int m = 0; m < 12; m++) {
                        ArrayList<String> entries = SeizureEntryHelper.loadSeizureList(this, selectedUserDirectoryName, choosenDate[0], m);
                        barChartValues[m] = entries.size();
                        if (barChartValues[m] > entriesMaxCount)
                            entriesMaxCount = barChartValues[m];
                    }
                }
                break;
            case TS_MONTH:
                int monthLength = 30 + (choosenDate[1]+(choosenDate[1]<7?1:0))%2;
                if(choosenDate[1]==1) {
                    if(choosenDate[0]%400==0 || (choosenDate[0]%100!=0 && choosenDate[0]%4==0)) {
                        monthLength = 29;
                    } else {
                        monthLength = 28;
                    }
                }
                Log.d(TAG,"calculated monthlength: "+monthLength);
                barChartValues = new int[monthLength];
                barChartLabels = new String[monthLength];
                if(hasPermission) {
                    ArrayList<String> entries = SeizureEntryHelper.loadSeizureList(this, selectedUserDirectoryName, choosenDate[0], choosenDate[1]);
                    for (String s : entries) {
                        SeizureHolder sh = new SeizureHolder(this, s);
                        barChartValues[sh.getSeizureDate()[2] - 1]++;
                    }
                }
                entriesMaxCount = 0;
                for(int d=0; d<monthLength; d++) {
                    barChartLabels[d] = ""+(d+1);
                    if(barChartValues[d]>entriesMaxCount) entriesMaxCount = barChartValues[d];
                }
                break;
            default: //return;
        }
    }
    private void fillBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for(int e=0; e<barChartValues.length; e++) {
            entries.add(new BarEntry(e, barChartValues[e]));
        }

        BarDataSet barDataSet = new BarDataSet(entries, "count");
        barDataSet.setColor(getResources().getColor(R.color.violet1));
        barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        barDataSet.setDrawValues(false);

        float granularity = 0.5f;
        if(entriesMaxCount>2) granularity = 1.0f;
        if(entriesMaxCount>5) granularity = 2.0f;
        if(entriesMaxCount>14) granularity = 2.5f;
        if(entriesMaxCount>29) granularity = 5.0f;
        if(entriesMaxCount>99) granularity = 10.0f;

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(-0.05f); // this replaces setStartAtZero(true)
        rightAxis.setGranularityEnabled(true);
        rightAxis.setGranularity(granularity);
        rightAxis.setValueFormatter(new IntegerValueFormatter());
        rightAxis.setTextSize(14f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-0.05f); // this replaces setStartAtZero(true)
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(granularity);
        leftAxis.setValueFormatter(new IntegerValueFormatter());
        leftAxis.setTextSize(14f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(-0.5f+barChartValues.length);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int iValue = (int) (value) + (value<0.0 ? barChartLabels.length : 0);
                iValue %= barChartLabels.length;
                return barChartLabels[iValue];
            }
        });

        Legend l = barChart.getLegend();
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setCustom(new LegendEntry[]{
                new LegendEntry(
                        getResources().getString(R.string.seizure_number_of) +
                                " "+ userList[userSpinner.getSelectedItemPosition()],
                        Legend.LegendForm.SQUARE,
                        Float.NaN, Float.NaN, null,
                        getResources().getColor(R.color.violet1)
                )
        });

        BarData barData = new BarData();
        barData.addDataSet(barDataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);

        barChart.invalidate();
        typeSpinnerHasListener = true;
    }
    private class IntegerValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            int iValue = (int) value;
            int i5value = (int) (value+0.5) - (value<-0.5 ? 1 : 0);
            return ""+(Math.abs(value-i5value)<0.1 ? ""+iValue : " ");
        }
    }

}
