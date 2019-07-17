package com.metzner.enrico.epilepsy.settings;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Locale;

import static android.content.pm.PackageManager.GET_META_DATA;

public class LocaleManager {

    public static final  String LANGUAGE_ENGLISH   = "en";
    public static final  String LANGUAGE_GERMAN    = "de";
    private static final String LANGUAGE_KEY       = "language_key";

    public static Context setLocale(Context c) {
        return updateResources(c, getLanguage(c));
    }

    public static Context setNewLocale(Context c, String language) {
        persistLanguage(c, language);
        return updateResources(c, language);
    }

    public static String getLanguage(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(LANGUAGE_KEY, LANGUAGE_ENGLISH);
    }

    @SuppressLint("ApplySharedPref")
    private static void persistLanguage(Context c, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        // use commit() instead of apply(), because sometimes we kill the application process immediately
        // which will prevent apply() to finish
        prefs.edit().putString(LANGUAGE_KEY, language).commit();
    }

    private static Context updateResources(@NonNull Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
//        if (Build.VERSION.SDK_INT >= 17) {
//            config.setLocale(locale);
//            context = context.createConfigurationContext(config);
//        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
//        }
        return context;
    }

    public static Locale getLocale(@NonNull Resources res) {
        Configuration config = res.getConfiguration();
        return Build.VERSION.SDK_INT >= 24 ? config.getLocales().get(0) : config.locale;
    }

    public static void resetTitle(@NonNull Activity callingActivity) {
        try {
            int label = callingActivity.getPackageManager().getActivityInfo(
                    callingActivity.getComponentName(), GET_META_DATA).labelRes;
            if (label != 0) {
                callingActivity.setTitle(label);
                ActionBar ab = callingActivity.getActionBar();
                if(ab!=null) ab.setTitle(label);
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
        }
    }

}