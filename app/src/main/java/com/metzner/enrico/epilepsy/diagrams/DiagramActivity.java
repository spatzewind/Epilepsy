package com.metzner.enrico.epilepsy.diagrams;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.OnSwipeTouchListener;
import com.metzner.enrico.epilepsy.settings.LocaleManager;

public class DiagramActivity extends AppCompatActivity {

    private static final String TAG = "DIAGRAM_ACTIVITY";




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
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
    }




    /**********************************************************************************************
     **** INITIALISATION **************************************************************************
     **********************************************************************************************/
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.resetTitle(this);
        setContentView(R.layout.activity_diagram);

        View diagramView = findViewById(R.id.diagram_relative_layout_view);
        diagramView.setOnTouchListener(new OnSwipeTouchListener(this, this) {
            @Override
            public void onSwipeRight() { finish(); }
        });

        ImageButton timeseriesButton = findViewById(R.id.dia_button_timeseries);
        timeseriesButton.setOnTouchListener(new OnSwipeTouchListener(this, this) {
            @Override
            public void onSwipeRight() { finish(); }

            @Override
            public void onClick() {
                Intent timeseriesIntent = new Intent(getContext(), TimeSeriesActivity.class);
                startActivity(timeseriesIntent);
            }
        });

        ImageButton piechartButton = findViewById(R.id.dia_button_piecharts);
        piechartButton.setOnTouchListener(new OnSwipeTouchListener(this, this) {
            @Override
            public void onSwipeRight() { finish(); }

            @Override
            public void onClick() {
                Intent piechartsIntent = new Intent(getContext(), PiechartsActivity.class);
                startActivity(piechartsIntent);
            }
        });
    }




    /**********************************************************************************************
     **** BUTTON INTERACTION **********************************************************************
     **********************************************************************************************/
    public void showCharts(View view) {
        Class chartClass;
        switch(view.getId()) {
            case R.id.dia_button_timeseries: chartClass = TimeSeriesActivity.class; break;
            case R.id.dia_button_piecharts: chartClass = PiechartsActivity.class; break;
            default: return;
        }
        Intent chartIntent = new Intent(this, chartClass);
        startActivity(chartIntent);
    }
}
