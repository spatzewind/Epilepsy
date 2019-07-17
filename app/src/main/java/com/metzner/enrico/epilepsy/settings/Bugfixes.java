package com.metzner.enrico.epilepsy.settings;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;

public class Bugfixes extends AppCompatActivity {
    //variables
    private static final String TAG = "BUGFIX_ACTIVITY";


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
        setContentView(R.layout.activity_bugfixes);

        ExpandableListView elvBfF = (ExpandableListView) findViewById(R.id.bf_f_listview);
        HashMap<String, List<CharSequence>> elvHashContent = new LinkedHashMap<>();
        elvHashContent.put("2.5.2", Arrays.asList(getResources().getTextArray(R.array.bfList_252)));
        elvHashContent.put("2.5.1", Arrays.asList(getResources().getTextArray(R.array.bfList_251)));
        CustomExpandableListAdapter cELV = new CustomExpandableListAdapter(this, elvHashContent);
        elvBfF.setAdapter(cELV);
    }




    public String[] getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        return getResources().getStringArray(resId);
    }


    public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

        private Context context;
        private List<String> expandableListTitle;
        private HashMap<String, List<CharSequence>> expandableListDetail;

        public CustomExpandableListAdapter(Context context, HashMap<String, List<CharSequence>> _expandableListDetail) {
            this.context = context;
            this.expandableListTitle = new ArrayList<String>(_expandableListDetail.keySet());
            this.expandableListDetail = _expandableListDetail;
        }

        @Override
        public Object getChild(int listPosition, int expandedListPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                    .get(expandedListPosition);
        }

        @Override
        public long getChildId(int listPosition, int expandedListPosition) {
            return expandedListPosition;
        }

        @Override
        public View getChildView(int listPosition, final int expandedListPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final CharSequence expandedListText = (CharSequence) getChild(listPosition, expandedListPosition);
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.bf_elv_item, null);
            }
            TextView expandedListTextView = (TextView) convertView
                    .findViewById(R.id.elvGroupListItem);
            expandedListTextView.setText(expandedListText);
            return convertView;
        }

        @Override
        public int getChildrenCount(int listPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                    .size();
        }

        @Override
        public Object getGroup(int listPosition) {
            return this.expandableListTitle.get(listPosition);
        }

        @Override
        public int getGroupCount() {
            return this.expandableListTitle.size();
        }

        @Override
        public long getGroupId(int listPosition) {
            return listPosition;
        }

        @Override
        public View getGroupView(int listPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            String listTitle = (String) getGroup(listPosition);
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.bf_elv_group, null);
            }
            TextView listTitleTextView = (TextView) convertView
                    .findViewById(R.id.elvGroupListTitle);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            listTitleTextView.setText(listTitle);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int listPosition, int expandedListPosition) {
            return true;
        }
    }
}
