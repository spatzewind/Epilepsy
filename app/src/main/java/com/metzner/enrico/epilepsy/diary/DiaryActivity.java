package com.metzner.enrico.epilepsy.diary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.epi_tools.OnSwipeTouchListener;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.seizure.NewSeizureEntry;
import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.seizure.SeizureHolder;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.users.UserHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class DiaryActivity extends AppCompatActivity {

    private static final String TAG = "DIARY_ACTIVITY";
    public static final String CALLING_ACTIVITY_ID = "DIARY_ACTIVITY";

    private String[] userStrings;
    private String[] userList;
    private File[] userDirectory;
    private String selectedUserString;
    private String selectedUserDirectoryName;
    private ListView seizureList;
    private int[] selectedDate;
    CalendarView datePicker;

    private Context diaryContext;




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
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
    }




    /**********************************************************************************************
     **** INITIALISATION **************************************************************************
     **********************************************************************************************/
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.resetTitle(this);
        setContentView(R.layout.activity_diary);

        View diaryView = findViewById(R.id.diary_relative_layout_view);
        diaryView.setOnTouchListener(new OnSwipeTouchListener(this,this) {
            @Override
            public void onSwipeLeft() { finish(); }
        });

        diaryContext = this;

        //get references
        datePicker = (CalendarView) findViewById(R.id.diary_date_picker);
        seizureList = (ListView) findViewById(R.id.diary_list_entries);
        seizureList.setOnItemClickListener(new DiaryListener());
        seizureList.setOnTouchListener(new OnSwipeTouchListener(this,this) {
            @Override
            public void onSwipeLeft() { finish(); }
        });
        selectedDate = new int[3]; //[year, month, day]

        //fill data
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(datePicker.getDate());
        selectedDate[0] = calendar.get(Calendar.YEAR);
        selectedDate[1] = calendar.get(Calendar.MONTH);
        selectedDate[2] = calendar.get(Calendar.DAY_OF_MONTH);
        fillUserList();
//        fillSeizureList();

        datePicker.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDate[0] = year;
                selectedDate[1] = month;
                selectedDate[2] = dayOfMonth;
                fillSeizureList();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        fillSeizureList();
    }





    private void fillUserList() {
        ArrayList<String> tempUsers = UserEntryHelper.loadUsers(this);
        boolean unassignedDataExist = SeizureEntryHelper.existNotAssignedData(this);
        int userCount = Math.max(1, tempUsers.size()+(unassignedDataExist?1:0));
        userList = new String[userCount];
        userStrings = new String[userCount];
        userDirectory = new File[userCount];
        for(int u=0; u<userCount; u++) {
            if(u<tempUsers.size()) {
                UserHolder tempUser = new UserHolder(this, tempUsers.get(u));
                userList[u] = tempUser.getName();
                userDirectory[u] = tempUser.getUserDir();
                userStrings[u] = tempUser.getUserEntryString();
            } else {
                userList[u] = getResources().getString(R.string.unknown_user);
                userDirectory[u] = null;
                userStrings[u] = UserEntryHelper.UserEntryFillString;
            }
        }
        int selectedUser = (unassignedDataExist ? userList.length-1 : 0);

        Spinner userSpinner = (Spinner) findViewById(R.id.diary_user_spinner);
        userSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, userList));
        userSpinner.setSelection(selectedUser);
        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(userList[position].equals(getResources().getString(R.string.unknown_user))) {
                    selectedUserString = UserEntryHelper.UserEntryFillString;
                    selectedUserDirectoryName = null;
                } else {
                    selectedUserString = userStrings[position];
                    selectedUserDirectoryName = userDirectory[position].getName();
                }
                fillSeizureList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing happens
            }
        });
    }
    private void fillSeizureList() {
        System.out.println("    Fill list of diary entries...");
        ArrayList<String> data;
        if ( PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            data = SeizureEntryHelper.loadSeizureList(this, selectedUserDirectoryName, selectedDate[0], selectedDate[1]);
        } else {
            data = new ArrayList<>();
            data.clear();
        }

        ArrayList<SeizureHolder> diaryItems = new ArrayList<>();
        diaryItems.clear();
        for (String s : data) {
            SeizureHolder temp_seizure_holder = new SeizureHolder(this, s);
            int[] seizureDate = temp_seizure_holder.getSeizureDate();
            if(seizureDate[2]!=selectedDate[2]) continue;
            System.out.println("Set diary-item: " + s);
            diaryItems.add(temp_seizure_holder);
        }
        if(diaryItems.size()==0) {
            diaryItems.add(new SeizureHolder(this, SeizureEntryHelper.SeizureEntryFillString));
        }
        seizureList.setAdapter(new DiaryAdapter(this, diaryItems));
    }

    private class DiaryViewHolder {
        ImageView symbol;
        TextView tv_seizure_type;
        TextView tv_description;
    }

    @SuppressWarnings("CanBeFinal")
    private class DiaryAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<SeizureHolder> listSeizure;

        DiaryAdapter(Context _context, ArrayList<SeizureHolder> entries) {
            context = _context;
            listSeizure = entries;
        }

        @Override
        public int getViewTypeCount() {
            return listSeizure.size();
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getCount() {
            return listSeizure.size();
        }
        @Override
        public Object getItem(int position) {
            return listSeizure.get(position);
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final DiaryViewHolder holder;

            if (convertView == null) {
                holder = new DiaryViewHolder();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                convertView = inflater.inflate(R.layout.imgtv_informativ_entry, null, true);

                holder.symbol = (ImageView) convertView.findViewById(R.id.imgtv_informative_entry_symbol);
                holder.tv_seizure_type = (TextView) convertView.findViewById(R.id.imgtv_informative_entry_title_title);
                holder.tv_description = (TextView) convertView.findViewById(R.id.imgtv_informative_entry_description);

                convertView.setTag(holder);
            }else {
                // the getTag returns the viewHolder object set as a tag to the view
                holder = (DiaryViewHolder)convertView.getTag();
            }

            //get content of diary entry
            SeizureHolder seizure_entry = listSeizure.get(position);
            String seizureTypeName = getResources().getString(R.string.diary_no_data);
            String diaryEntryDescriptionText = "...";
            int diary_symbel_imageID = R.drawable.seizuretype_unknown;
            boolean isStatus = false;
            if(!seizure_entry.getSeizureEntryString().equals("-9999")) {
                int seizureTypeID = seizure_entry.getSeizureTypeID();
                isStatus = seizure_entry.isSeizureStatus();
                seizureTypeName = getResources().getStringArray(R.array.seizure_type_list)[seizureTypeID];
                int[] seizureTime = seizure_entry.getSeizureTime();
                long seizureDuration = seizure_entry.getSeizureDuration();
                diaryEntryDescriptionText = SeizureEntryHelper.time2string(seizureTime, context) + " , " +
                        getResources().getString(R.string.ns_duration) + ": " +
                        SeizureEntryHelper.duration2string(seizureDuration, true);
                diary_symbel_imageID = SeizureEntryHelper.seizure_type_symbols_ids[seizureTypeID];
            }

            //set content of diary entry
            holder.symbol.setImageDrawable(getDrawable(diary_symbel_imageID));
            holder.tv_seizure_type.setText(seizureTypeName);
            holder.tv_description.setText(diaryEntryDescriptionText);
            if(isStatus)
                convertView.setBackgroundColor(getResources().getIntArray(R.array.colors_pie_chart)[15]);

            return convertView;
        }
    }

    private class DiaryListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //System.out.println("Clicked on item: "+position+" of possible 0-"+(parent.getCount()-1));
            SeizureHolder entry = (SeizureHolder) parent.getAdapter().getItem(position);
            String entryString = entry.getSeizureEntryString();
            if(entry.getSeizureEntryString().equals(SeizureEntryHelper.SeizureEntryFillString)) {
                entry.setSeizureDate(selectedDate[0], selectedDate[1], selectedDate[2]);
                entryString = entry.createSeizureEntryString(diaryContext, false);
            }
            Intent editorIntent = new Intent(diaryContext, NewSeizureEntry.class);
            editorIntent.putExtra("seizureEntry", entryString);
            editorIntent.putExtra(NewSeizureEntry.USERSTRING_EXTRA_ID, selectedUserString);
            editorIntent.putExtra("callingActivityID", CALLING_ACTIVITY_ID);
            startActivity(editorIntent);
        }
    }
}
