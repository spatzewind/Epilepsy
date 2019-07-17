package com.metzner.enrico.epilepsy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.metzner.enrico.epilepsy.diagrams.DiagramActivity;
import com.metzner.enrico.epilepsy.diary.DiaryActivity;
import com.metzner.enrico.epilepsy.epi_tools.OnSwipeTouchListener;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.seizure.NewSeizureEntry;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.settings.About;
import com.metzner.enrico.epilepsy.settings.Bugfixes;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.settings.Settings;
import com.metzner.enrico.epilepsy.users.NewUser;
import com.metzner.enrico.epilepsy.users.UserHolder;
import com.metzner.enrico.epilepsy.users.UserOverview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    //variables
    private static final String TAG = "MAIN_ACTIVITY";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    //private TextView timeSinceLastSeizure;
    private int entriesMaxCount;
    private ArrayList<String> users;
    private ArrayList<LineDataSet> dataSets;
    private ArrayList<LegendEntry> legendEntries;
    //private String[] lineStarts;
    private String[] xAxis_dates;
    private LineChart time_series;

    private Context mainContext = this;
    private Locale currentLocale;

    private int askForWritePermission;



    

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
        setContentView(R.layout.activity_main);
        final View contentView = findViewById(R.id.main_relative_layout_view);
        contentView.setOnTouchListener(new OnSwipeTouchListener(this, this) {
            @Override
            public void onSwipeLeft() {
                Intent diagramIntent = new Intent(this.getContext(), DiagramActivity.class);
                startActivity(diagramIntent);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
            }
            @Override
            public void onSwipeRight() {
                Intent diaryIntent = new Intent(this.getContext(), DiaryActivity.class);
                startActivity(diaryIntent);
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
            }
        });

        currentLocale = getResources().getConfiguration().locale;

        // check permission
        if ( ! PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            PermissionsHelper.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE,
                    getResources().getString(R.string.permission_write_external_storage_description), 0);
        }

        //check for user data
        checkPropertyFile();

        //get references
        //timeSinceLastSeizure = (TextView) findViewById(R.id.elapsedTimeView);
        time_series = (LineChart) findViewById(R.id.linechart);
    }
    @Override
    protected void onResume() {
        super.onResume();
        askForWritePermission = 0;
        checkPropertyFile();
        getUsers();
        getTimeSeriesData();
        fillLineChart();
    }




    /**********************************************************************************************
     **** MENU ************************************************************************************
     **********************************************************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_diary:
                Intent diaryIntent = new Intent(this, DiaryActivity.class);
                startActivity(diaryIntent);
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
                return true;
            case R.id.menu_diagrams:
                Intent diagramIntent = new Intent(this, DiagramActivity.class);
                startActivity(diagramIntent);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
                return true;
            case R.id.menu_users:
                Intent usersIntent = new Intent(this, UserOverview.class);
                startActivity(usersIntent);
                return true;
            case R.id.menu_settings:
                Intent settingIntent = new Intent(this, Settings.class);
                startActivity(settingIntent);
                return true;
            case R.id.menu_about:
                Intent aboutIntent = new Intent(this, About.class);
                startActivity(aboutIntent);
                return true;
            case R.id.menu_bugfixes:
                Intent bugfixIntent = new Intent(this, Bugfixes.class);
                startActivity(bugfixIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




    /**********************************************************************************************
     **** PERMISSION REQUEST **********************************************************************
     **********************************************************************************************/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent i = new Intent(this, MainActivity.class);
                    startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    askForWritePermission = 0;
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }




    /**********************************************************************************************
     **** TIME-SERIES LINE-CHART ******************************************************************
     **********************************************************************************************/
    private void getUsers() {
        users = new ArrayList<>();
        users.clear();
        users = UserEntryHelper.loadUsers(this);
    }
    private void getTimeSeriesData() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month2 = (month+11)%12;
        int year2 = (month2>month ? year-1 : year);

        //prepare Axis
        String[] lineStarts = new String[7];
        xAxis_dates = new String[7];
        for(int t=0; t<7; t++) {
            int new_day = day-t;
            int new_month = month;
            int new_year = year;
            if(new_day<1) {
                new_month = (month+11)%12;
                if(new_month>month) new_year = year-1;
                switch(new_month) {
                    case 0: new_day += 31; break; //january
                    case 1: new_day += (new_year%4==0 ? 29 : 28); break; //february
                    case 2: new_day += 31; break; //march
                    case 3: new_day += 30; break; //april
                    case 4: new_day += 31; break; //mai
                    case 5: new_day += 30; break; //june
                    case 6: new_day += 31; break; //july
                    case 7: new_day += 31; break; //august
                    case 8: new_day += 30; break; //september
                    case 9: new_day += 31; break; //october
                    case 10: new_day += 30; break; //november
                    case 11: new_day += 31; break; //december
                }
            }
            lineStarts[6-t] = ""+new_year+""+(new_month>8 ? "" : "0")+(new_month+1)+""+(new_day>9 ? "" : "0")+new_day;
            xAxis_dates[6-t] = SeizureEntryHelper.date2string(new int[]{new_year,new_month,new_day},
                    null, currentLocale, this, true);
            Log.d(TAG, "Entry("+(6-t)+"): <"+lineStarts[6-t]+"  "+xAxis_dates[6-t]+">");
        }

        //read date
        int numberOfUsers = users.size();
        int unknownUserData = (SeizureEntryHelper.existNotAssignedData(this) ? 1 : 0);
        int numberOfLines = Math.max(1, numberOfUsers+unknownUserData);
        dataSets = new ArrayList<>();
        dataSets.clear();
        legendEntries = new ArrayList<>();
        legendEntries.clear();
        entriesMaxCount = 0;
        for(int l=0; l<numberOfLines; l++) {
            ArrayList<String> seizures = new ArrayList<>();
            String userDir = null;
            String userName = getResources().getString(R.string.unknown_user);
            int userColour = getResources().getColor(R.color.violet1);
            if(l<numberOfUsers) {
                UserHolder user = new UserHolder(this, users.get(l));
                userName = user.getName();
                userDir = user.getUserDir().getName();
                userColour = user.getColour();
            }
            if( PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
                Log.d(TAG, "Load data from <" + year2 + "|" + month2 + ">-file ...");
                seizures.addAll(SeizureEntryHelper.loadSeizureList(this, userDir, year2, month2));
                Log.d(TAG, "Load data from <" + year + "|" + (month + 1) + ">-file ...");
                seizures.addAll(SeizureEntryHelper.loadSeizureList(this, userDir, year, month));
            }

            int[] counter = new int[7];
            for (int c = 0; c < 7; c++) counter[c] = 0;
            Log.d(TAG, "Number of loaded seizures entries: " + seizures.size());
            for (String entry : seizures) {
//                Log.d(TAG, "seizure entry: " + entry);
                for (int t = 0; t < 7; t++) {
                    if (entry.startsWith(lineStarts[t])) {
                        counter[t]++;
//                        Log.d(TAG, "Entry: <" + lineStarts[t] + "  " + counter[t] + "  " + xAxis_dates[t] + ">");
                    }
                }
            }
            ArrayList<Entry> lineEntries = new ArrayList<>();
            lineEntries.clear();
            for (int t = 0; t < 7; t++) {
                lineEntries.add(new Entry(t - 6, counter[t]));
                if (counter[t] > entriesMaxCount) entriesMaxCount = counter[t];
                Log.d(TAG, lineEntries.get(t).toString());
            }


            LineDataSet lineDataSet = new LineDataSet(lineEntries, "Line");
            lineDataSet.setColor(userColour);
            lineDataSet.setLineWidth(2.5f);
            lineDataSet.setDrawCircles(true);
            lineDataSet.setCircleColor(userColour);
            lineDataSet.setCircleRadius(3.0f);
            lineDataSet.setFillColor(userColour);
            lineDataSet.setMode(LineDataSet.Mode.LINEAR);
            lineDataSet.setDrawValues(false);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSets.add(lineDataSet);

//            String legendLabelString = getResources().getString(R.string.seizure_number_of) + " " + userName;
            String legendLabelString = userName;
            LegendEntry legendEntry = new LegendEntry(
                    legendLabelString, Legend.LegendForm.LINE,
                    Float.NaN, Float.NaN, null,
                    userColour
            );
            legendEntries.add(legendEntry);
        }
    }
    private void fillLineChart() {

        float granularity = 0.5f;
        if(entriesMaxCount>2) granularity = 1.0f;
        if(entriesMaxCount>5) granularity = 2.0f;
        if(entriesMaxCount>14) granularity = 2.5f;

        YAxis rightAxis = time_series.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(-0.05f); // this replaces setStartAtZero(true)
        rightAxis.setGranularityEnabled(true);
        rightAxis.setGranularity(granularity);
        rightAxis.setValueFormatter(new IntegerValueFormatter());
        rightAxis.setTextSize(14f);

        YAxis leftAxis = time_series.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-0.05f); // this replaces setStartAtZero(true)
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(granularity);
        leftAxis.setValueFormatter(new IntegerValueFormatter());
        leftAxis.setTextSize(14f);

        XAxis xAxis = time_series.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(-6f);
        xAxis.setAxisMaximum(0f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int iValue = (int) (value+13);
                iValue %= xAxis_dates.length;
                return xAxis_dates[iValue];
            }
        });

        LegendEntry[] lEntries = new LegendEntry[legendEntries.size()];
        if(lEntries.length>0) {
            for(int le=0; le<lEntries.length; le++) lEntries[le] = legendEntries.get(le);
            Legend l = time_series.getLegend();
            l.setWordWrapEnabled(true);
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setCustom(lEntries);
        }

        LineData lineData = new LineData();
        for(LineDataSet lds: dataSets) lineData.addDataSet(lds);
        time_series.setData(lineData);
        time_series.getDescription().setText("");

        time_series.invalidate();
    }
    private class IntegerValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            int iValue = (int) value;
            int i5value = (int) (value+0.5) - (value<-0.5 ? 1 : 0);
            return ""+(Math.abs(value-i5value)<0.1 ? ""+iValue : " ");
        }
    }




    /**********************************************************************************************
     **** CHECK FOR VERSION AND USERS *************************************************************
     **********************************************************************************************/
    private void checkPropertyFile() {
        if( ! PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
            return;

        File docFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if(!docFolder.exists())
            if(!docFolder.mkdir()) {
                Log.d(TAG, "checkPropertyFile: Error during creation of folder <"+docFolder.getAbsolutePath()+">");
                return;
            }

        File newFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Epilepsy");
        if(!newFolder.exists())
            if(!newFolder.mkdir()) {
                Log.d(TAG, "checkPropertyFile: Error during creation of folder <"+newFolder.getAbsolutePath()+">");
                return;
            }

        //copy data files from old saving directory (older version) to new location
        File oldFolder = new File(getResources().getString(R.string.data_path));
        if(oldFolder.exists()) {
            Log.d(TAG, "checkPropertyFile: old Folder = <"+oldFolder.getAbsolutePath()+">");
            try {
                if(!newFolder.exists()) {
                    if(!newFolder.mkdir()) throw new IOException("Could not create <"+newFolder.getAbsolutePath()+">!");
                }
                copyFilesRecursive(oldFolder, newFolder);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        Log.d(TAG, "checkPropertyFile: new Folder = <"+newFolder.getAbsolutePath()+">");

        //delete files, which will never used again
        String[] filesToDelete = new String[]{
                "epi_profil.txt",
                "seizureTypes.txt",
                "warningTypes.txt",
                "triggerTypes.txt"
        };
        for(String s: filesToDelete) {
            File f = new File(newFolder, s);
            if(f.exists()) {
                if(!f.delete()) {
                    Log.d(TAG, "checkPropertyFile: Could not delete <"+f.getAbsolutePath()+">");
                }
            } else {
                Log.d(TAG, "checkPropertyFile: File <"+f.getAbsolutePath()+"> does not exist.");
            }
        }

        //check if "users.epi" exists
        final File userFile = new File(newFolder, "users.epi");
        if(!userFile.exists()) {
            final View noUserInfo = findViewById(R.id.main_activity_no_user_overlay);
            noUserInfo.setVisibility(View.VISIBLE);
            Button createButton = (Button) findViewById(R.id.main_no_user_create);
            createButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noUserInfo.setVisibility(View.INVISIBLE);
                    Intent newUserIntent = new Intent(mainContext, NewUser.class);
                    newUserIntent.putExtra(NewUser.ENTRYSTATE_EXTRA_ID, NewUser.ENTRYSTATE_NEW);
                    startActivity(newUserIntent);
                }
            });
            Button laterButton = (Button) findViewById(R.id.main_no_user_later);
            laterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noUserInfo.setVisibility(View.INVISIBLE);
                    UserEntryHelper.saveUsers(new ArrayList<String>());
                }
            });
        }
    }
    private void copyFilesRecursive(@NonNull File src, @NonNull File dst) throws IOException {
        File[] files = src.listFiles();
        for(File f: files) {
            if(f.isDirectory()) {
                File newDir = new File(dst, f.getName());
                if(newDir.mkdir()) {
                    copyFilesRecursive(f, newDir);
                } else {
                    Log.d(TAG, "copyFilesRecursive: Could not create directory <"+newDir.getAbsolutePath()+">");
                    throw new IOException("Could not create directory <"+newDir.getAbsolutePath()+">");
                }
            } else {
                Log.d(TAG, "copyFilesRecursive: try to copy <"+f.getAbsolutePath()+">...");
                File newFile = new File(dst, f.getName());
                try (InputStream in = new FileInputStream(f)) {
                    try (OutputStream out = new FileOutputStream(newFile)) {
                        // Transfer bytes from in to out
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                }
                if(newFile.exists()) {
                    if(newFile.length()==f.length()) {
                        if(!f.delete()) {
                            Log.d(TAG, "copyFilesRecursive: Could not delete directory <"+f.getAbsolutePath()+">");
                            throw new IOException("Could not delete directory <"+f.getAbsolutePath()+">");
                        } else {
                            Log.d(TAG, "copyFilesRecursive: delete <"+f.getAbsolutePath()+">...");
                        }
                    }
                }
            }
        }
        if(!src.delete()) {
            Log.d(TAG, "copyFilesRecursive: Could not delete directory <"+src.getAbsolutePath()+">");
            throw new IOException("Could not delete directory <"+src.getAbsolutePath()+">");
        } else {
            Log.d(TAG, "copyFilesRecursive: delete <"+src.getAbsolutePath()+">...");
        }
    }




    /**********************************************************************************************
     **** ADD NEW SEIZURE ENTRY *******************************************************************
     **********************************************************************************************/
    public void addNewSeizure(View view) {
        if( PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            Intent seizureIntent = new Intent(this, NewSeizureEntry.class);
            seizureIntent.putExtra("callingActivityID", "MAIN_ACTIVITY");
            startActivity(seizureIntent);
        } else {
            askForWritePermission++;
            PermissionsHelper.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE,
                    getResources().getString(R.string.permission_write_external_storage_description),
                    askForWritePermission);
        }
    }
}