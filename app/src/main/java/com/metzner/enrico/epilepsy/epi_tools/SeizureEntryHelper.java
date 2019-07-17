package com.metzner.enrico.epilepsy.epi_tools;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.metzner.enrico.epilepsy.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public abstract class SeizureEntryHelper {

    private static final String TAG = "SEIZURE_ENTRY_HELPER";

    public static final String SeizureEntryFillString = "-9999";
    public static int[] seizure_type_symbols_ids = new int[]{
                R.drawable.seizuretype_unknown,     //not known
                R.drawable.seizuretype_absence,     //Absence
                R.drawable.seizuretype_atonic,      //Atonic
                R.drawable.seizuretype_absence,     //Atypical Absence
                R.drawable.seizuretype_aura_only,   //Aura only
                R.drawable.seizuretype_clonic,      //Clonic
                R.drawable.seizuretype_partial,     //Complex Partial
                R.drawable.seizuretype_unknown,     //Myoclonic
                R.drawable.seizuretype_unknown,     //Myoclonic Cluster
                R.drawable.seizuretype_unknown,     //Gelastic
                R.drawable.seizuretype_unknown,     //Infantile Spasm
                R.drawable.seizuretype_generalized, //Secondarily Generalized
                R.drawable.seizuretype_partial,     //Simple Partial
                R.drawable.seizuretype_tonic,       //Tonic
                R.drawable.seizuretype_tonic_clonic //Tonic-Clonic
    };

    public static ArrayList<String> loadSeizureList(Context context, @Nullable String username, int _year, int _month) {
        ArrayList<String> lines = new ArrayList<>();
        lines.clear();
        if( !PermissionsHelper.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
            return lines;

        String _file_name = "Epilepsy/"+(username!=null ? username+"/" : "")+"data" + _year + (_month>8 ? "" : "0") + (_month+1) + ".txt";
        File _saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), _file_name);
        String _file_path = _saveFile.getAbsolutePath();
        Log.d(TAG, "Try open file path: <"+_file_path+">...");
        File data_file = new File(_file_path);
        if( !data_file.exists() )
            return lines;
        try {
            FileInputStream fis = new FileInputStream(data_file);
            InputStreamReader iss = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(iss);
            while(true) {
                String line = br.readLine();
                if(line==null) break;
                lines.add(line);
            }
            fis.close();
            iss.close();
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return lines;
    }
    public static boolean saveSeizureList(Context context, @Nullable String username, int _year, int _month, @NonNull ArrayList<String> seizure_list) {
        if( !PermissionsHelper.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
            return false;

        boolean hasWorked = true;
        String _file_name = "Epilepsy/"+(username!=null ? username+"/" : "") + "data" + _year + (_month>8 ? "" : "0") + (_month+1) + ".txt";
        File _saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), _file_name);
        String _file_path = _saveFile.getAbsolutePath();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(_file_path));
            int lineCounter = 0;
            for(String entry: seizure_list) {
                if(lineCounter!=0) bw.newLine();
                bw.write(entry);
                lineCounter++;
            }
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            hasWorked = false;
        }
        return hasWorked;
    }

    public static boolean existNotAssignedData(Context context) {
        if( !PermissionsHelper.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
            return false;
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Epilepsy");
        if(!folder.exists()) return false;
        File[] files = folder.listFiles();
        for(File f: files) {
            String fName = f.getName();
            if(fName.length()==14 && fName.startsWith("data") && fName.endsWith(".txt")) {
                Log.d(TAG, "existNotAssignedData: File <"+f.getAbsolutePath()+"> found...");
                return true;
            }
        }
        return false;
    }

    public static String date2string(@NonNull int[] date, @Nullable String template, @NonNull Locale locale, @NonNull Context context, boolean... shortVersion) {
        String yearString = ""+date[0]+"";
        String[] months = context.getResources().getStringArray(R.array.monthAbbr);
        String dayString = ""+date[2];
        boolean useDifferentLocale = false;
        if(locale.equals(Locale.GERMAN) || locale.equals(Locale.GERMANY)) {
            dayString += "."; useDifferentLocale = true;
        }
        if(!useDifferentLocale) {
            dayString += (date[2]==1 ? "st" :
                          (date[2]==2 ? "nd" :
                            (date[2]==3 ? "rd" : "th")));
        }



        String dateString = context.getResources().getString(R.string.dateFormat);
        if(template!=null) dateString = template;
        if(shortVersion.length>0 && shortVersion[0]) dateString = context.getResources().getString(R.string.dateFormat_short);
        dateString = dateString.replace("dd", dayString);
        dateString = dateString.replace("MMM", months[date[1]]);
        dateString = dateString.replace("yyyy", yearString);
        return dateString;
    }
    public static String time2string(@NonNull int[] time, @NonNull Context context) {
        int hh = time[0]%12;
        int HH = time[0];
        int mm = time[1];
        String am_pm = (HH>11 ? "pm" : "am");
        String hour12 = (hh>9 ? "" : " ") + hh;
        String hour24 = (HH>9 ? "" : " ") + HH;
        String min60 = (mm>9 ? "" : "0") + mm;

        String timeString = context.getResources().getString(R.string.timeFormat);
        timeString = timeString.replace("HH", hour24);
        timeString = timeString.replace("hh", hour12);
        timeString = timeString.replace("mm", min60);
        timeString = timeString.replace("a", am_pm);
        return timeString;
    }
    public static String duration2string(long duration, boolean shortVariant) {
        String durationString;
        int hours = (int) (duration / 3600L);
        int minutes = (int) ((duration - 3600L * hours) / 60L);
        int seconds = (int) (duration - 3600L * hours - 60L * minutes);
        if ( shortVariant ) {
            durationString = seconds+"sec";
            if(minutes>0) durationString = minutes+"min";
            if(hours>0) durationString = hours+"h";
        } else {
            durationString = (hours > 99 ? "" : (hours > 9 ? " " : "  ")) + hours;
            durationString += " :" + (minutes > 9 ? " " : " 0") + minutes;
            durationString += " :" + (seconds > 9 ? " " : " 0") + seconds;
        }
        return durationString;
    }
}
