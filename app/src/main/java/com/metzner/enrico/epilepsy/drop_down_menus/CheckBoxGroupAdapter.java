package com.metzner.enrico.epilepsy.drop_down_menus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.R;

import java.util.ArrayList;

@SuppressWarnings("CanBeFinal")
public class CheckBoxGroupAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<CheckBoxGroupItem> checkBoxList;


    public CheckBoxGroupAdapter(Context context, ArrayList<CheckBoxGroupItem> _checkBoxList) {

        this.context = context;
        this.checkBoxList = _checkBoxList;

    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return checkBoxList.size();
    }

    @Override
    public Object getItem(int position) {
        return checkBoxList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            convertView = inflater.inflate(R.layout.checkbox_group_item, null, true);

            holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkboxGroup_item_checkbox);
            holder.textView = (TextView) convertView.findViewById(R.id.checkboxGroup_item_text);
//            holder.checkBox
//                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//                        @Override
//                        public void onCheckedChanged(CompoundButton buttonView,
//                                                     boolean isChecked) {
//                            CheckBox element = (CheckBox) holder.checkBox
//                                    .getTag();
//                            element.setSelected(buttonView.isChecked());
//
//                        }
//                    });
            convertView.setTag(holder);
            holder.checkBox.setTag(checkBoxList.get(position));
            convertView.setTag(holder);
        }else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = (ViewHolder)convertView.getTag();
            ((ViewHolder) convertView.getTag()).checkBox.setTag(checkBoxList.get(position));
        }


//        holder.checkBox.setText("Checkbox "+position);
        holder.checkBox.setText("");
        holder.textView.setText(checkBoxList.get(position).getText());

        holder.checkBox.setChecked(checkBoxList.get(position).isChecked());

        holder.checkBox.setTag(R.integer.btnplusview, convertView);
        holder.checkBox.setTag( position);

        checkBoxList.get(position).setCheckBox(holder.checkBox);

        return convertView;
    }

    private class ViewHolder {

        CheckBox checkBox;
        private TextView textView;

    }
}
