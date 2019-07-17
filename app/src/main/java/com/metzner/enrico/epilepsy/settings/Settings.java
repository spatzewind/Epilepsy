package com.metzner.enrico.epilepsy.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.metzner.enrico.epilepsy.MainActivity;
import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.drop_down_menus.ImageTextSpinnerAdapter;
import com.metzner.enrico.epilepsy.drop_down_menus.ImageTextSpinnerSelectionListener;

public class Settings extends AppCompatActivity {
    private static final String TAG = "SETTINGS_ACTIVITY";
    private String language_at_start;
    private Spinner language_spinner;
    private ImageTextSpinnerAdapter languageAdapter;





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
        setContentView(R.layout.activity_settings);


        int[] language_drawables_ids = new int[] {
                R.drawable.language_en,
                R.drawable.language_de
        };
        String[] language_titles = getResources().getStringArray(R.array.languages);

        languageAdapter = new ImageTextSpinnerAdapter(this, language_titles, language_drawables_ids, false);
        language_spinner = (Spinner) findViewById(R.id.row_language_choice);
        language_spinner.setAdapter(languageAdapter);
        language_spinner.setOnItemSelectedListener(new ImageTextSpinnerSelectionListener());
        language_at_start = LocaleManager.getLanguage(this);
        language_spinner.setSelection(languageAdapter.getPositionOfLanguage(language_at_start));
    }




    /**********************************************************************************************
     **** INTERACTIONS ****************************************************************************
     **********************************************************************************************/
    public void applySettings(View view) {
        String probably_new_language = languageAdapter.getItem(language_spinner.getSelectedItemPosition());
        if(probably_new_language!=null) probably_new_language = probably_new_language.substring(0,2);
        if(! language_at_start.equals(probably_new_language)) {
            LocaleManager.setNewLocale(this, probably_new_language);

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));

            Toast.makeText(this, "Activity restarted", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}