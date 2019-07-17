package com.metzner.enrico.epilepsy.drop_down_menus;

import android.view.View;
import android.widget.AdapterView;

public class ImageTextSpinnerSelectionListener implements AdapterView.OnItemSelectedListener {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ImageTextSpinnerAdapter itsa = (ImageTextSpinnerAdapter) parent.getAdapter();
        itsa.setSelectedPosition(position);
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //return;
    }
}
