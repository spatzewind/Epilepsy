package com.metzner.enrico.epilepsy.drop_down_menus;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.R;

public class ImageTextSpinnerAdapter extends ArrayAdapter<String> {

    private static final String TAG = "I-T-S_ADAPTER";

    private Context context;
    private int layout_resource_id,
                layout_resource_id2;
    private int[] drawables_ids;
    private String[] titles;
    private int selectedPosition;
    private boolean shouldTextShortend;

    public ImageTextSpinnerAdapter(@NonNull Context _context, String[] entry_texts, int[] entry_imageIDs, boolean shortend) {
        super(_context, R.layout.spinner_image_textview_row_aligncenter);
        context = _context;
        layout_resource_id = R.layout.spinner_image_textview_row_aligncenter;
        layout_resource_id2 = R.layout.spinner_image_textview_row_alignstart;
        drawables_ids = entry_imageIDs;
        titles = entry_texts;
        selectedPosition = -1;
        shouldTextShortend = shortend;
    }


    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public String getItem(int position) {
        return titles[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater mInflater = (LayoutInflater) context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(layout_resource_id, parent, false);
            holder.iv_language_symbol = (ImageView) convertView.findViewById(R.id.spinner_imagetextview_row_symbol);
            holder.tv_language = (TextView) convertView.findViewById(R.id.spinner_imagetextview_row_title);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.iv_language_symbol.setImageResource(drawables_ids[position]);
        String languageText = titles[position];
        if(shouldTextShortend && languageText.length()>27) languageText = languageText.substring(0,24) + "...";
        holder.tv_language.setText(languageText);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//        Log.d(TAG, "getDropDownView: selectedPosition="+selectedPosition);
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            LayoutInflater mInflater = (LayoutInflater) context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(layout_resource_id2, parent, false);
            holder.cl_language = (ConstraintLayout) convertView.findViewById(R.id.language_spinner_row2);
            holder.iv_language_symbol = (ImageView) convertView.findViewById(R.id.spinner_imagetextview_row_symbol2);
            holder.tv_language = (TextView) convertView.findViewById(R.id.spinner_imagetextview_row_title2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(position==selectedPosition) {
            holder.cl_language.setBackgroundColor(parent.getResources().getColor(R.color.highlight_violet));
        } else {
            holder.cl_language.setBackgroundColor(Color.TRANSPARENT);
        }
        holder.iv_language_symbol.setImageResource(drawables_ids[position]);
        holder.tv_language.setText(titles[position]);

        return convertView;
    }

    public int getPositionOfLanguage(String language) {
        for(int p = 0; p< titles.length; p++) {
            if (titles[p].startsWith(language)) return p;
        }
        return 0;
    }

    public void setSelectedPosition(int _position) {
        selectedPosition = _position;
    }

    private class ViewHolder {
        ConstraintLayout cl_language;
        ImageView iv_language_symbol;
        TextView tv_language;
    }
}
