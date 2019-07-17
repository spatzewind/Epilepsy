package com.metzner.enrico.epilepsy.drop_down_menus;

import android.widget.CheckBox;

public class CheckBoxGroupItem {

    private CheckBox checkBox;
    private boolean isChecked;
    private String text;

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public void setCheckBox(CheckBox _checkBox) {
        checkBox = _checkBox;
    }

    public String getText() {
        return text;
    }

    public void setText(String _text) {
        this.text = _text;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
