package com.metzner.enrico.epilepsy.drop_down_menus;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;

@SuppressWarnings("CanBeFinal")
public class CheckBoxGroupListener implements OnItemClickListener {

    private int number_of_first_singletons;

    public CheckBoxGroupListener(int _nofs) {
        number_of_first_singletons = _nofs;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        System.out.println("Clicked on item: "+position+" of possible 0-"+(parent.getCount()-1));
        if(position<number_of_first_singletons) {
            int viewCount = parent.getAdapter().getCount();
            viewCount = Math.min(viewCount, parent.getCount());
            for(int vc=0; vc<viewCount; vc++) {
                setItemChecked(parent, vc, (vc==position));
            }
        } else {
            for(int s=0; s<number_of_first_singletons; s++) {
                setItemChecked(parent, s, false);
            }
            CheckBoxGroupItem cbgItem = (CheckBoxGroupItem) parent.getAdapter().getItem(position);
            setItemChecked(parent, position, (cbgItem == null || !cbgItem.isChecked()));
        }
    }

    private void setItemChecked(@NonNull AdapterView<?> adapterView, int _position, boolean isChecked) {
        int count = adapterView.getCount();
        System.out.println("    check view at position "+_position+" of "+count);
//        View selectedView = adapterView.getChildAt(_position);
//        if(selectedView != null) {
//            CheckBox selectedCheckBox = (CheckBox) selectedView.findViewById(R.id.checkboxGroup_item_checkbox);
//            selectedCheckBox.setChecked(isChecked);
//        }
        CheckBoxGroupItem selectedItem = (CheckBoxGroupItem) adapterView.getAdapter().getItem(_position);
        if(selectedItem!=null) {
            selectedItem.setChecked(isChecked);
            CheckBox selCB = selectedItem.getCheckBox();
            if(selCB!=null) selCB.setChecked(isChecked);
        }
    }

}
