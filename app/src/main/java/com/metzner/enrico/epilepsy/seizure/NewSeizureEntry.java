package com.metzner.enrico.epilepsy.seizure;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TimePicker;
import android.widget.Toast;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.diary.DiaryActivity;
import com.metzner.enrico.epilepsy.drop_down_menus.CheckBoxGroupAdapter;
import com.metzner.enrico.epilepsy.drop_down_menus.CheckBoxGroupItem;
import com.metzner.enrico.epilepsy.drop_down_menus.CheckBoxGroupListener;
import com.metzner.enrico.epilepsy.drop_down_menus.ImageTextSpinnerAdapter;
import com.metzner.enrico.epilepsy.drop_down_menus.ImageTextSpinnerSelectionListener;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.users.UserHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class NewSeizureEntry extends AppCompatActivity {
    //variables
    private static final int maxStringLength = 30;
    private static final String TAG = "NEW_SEIZURE_ACTIVITY";
    public static final String USERSTRING_EXTRA_ID = "userEntry";
    public static final String SEIZURESTRING_EXTRA_ID = "seizureEntry";

    private Locale currentLocale;

    private DatePicker datePicker;
    private TimePicker timePicker;
    private long seizurePeriod_StartTime, seizurePeriod_EndTime;
    private boolean runTimer;
    private Thread seizurePeriodTimer;
    private NumberPicker hoursPicker, minutesPicker, secondsPicker;
    private Spinner userSpinner,seizureTypePicker,emergencyPicker,activityPicker;
    private CheckBoxGroupAdapter warningAdapter, triggerAdapter;
    private EditText notesPicker;
    private Button date_button, time_button, duration_button,
                   warning_button, trigger_button, notes_button;
    private String[] userList;
    private File[] userDirectory;
    private String callerID;
    private SeizureHolder seizureEntry;
    private String init_user_string;
    private String init_seizure_entry_string;
    private int subInterfaceID = Integer.MAX_VALUE;
    private InputMethodManager imm;





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
        setContentView(R.layout.activity_new_seizure_entry);
        currentLocale = getResources().getConfiguration().locale;
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        seizurePeriod_StartTime = System.currentTimeMillis();

        TableRow tr_button_newentry = (TableRow) findViewById(R.id.button_line_new);
        TableRow tr_button_diary = (TableRow) findViewById(R.id.button_line_diary);

        ImageTextSpinnerAdapter seizureTypeAdapter = new ImageTextSpinnerAdapter(this,
                getResources().getStringArray(R.array.seizure_type_list),
                SeizureEntryHelper.seizure_type_symbols_ids, true);
        String[] warningStrings = getResources().getStringArray(R.array.ns_warning_list);
        boolean[] warningChecks = new boolean[warningStrings.length];
        for(int b=0; b<warningStrings.length; b++) warningChecks[b] = false;
        String[] triggerStrings = getResources().getStringArray(R.array.ns_trigger_list);
        boolean[] triggerChecks = new boolean[triggerStrings.length];
        for(int b=0; b<triggerStrings.length; b++) triggerChecks[b] = false;

        //get users
        ArrayList<String> tempUsers = UserEntryHelper.loadUsers(this);
        boolean unassignedDataExist = SeizureEntryHelper.existNotAssignedData(this);
        int userCount = Math.max(1, tempUsers.size()+(unassignedDataExist?1:0));
        userList = new String[userCount];
        userDirectory = new File[userCount];
        int[] userImageIDs = new int[userCount];
        for(int u=0; u<userCount; u++) {
            if(u<tempUsers.size()) {
                UserHolder tempUser = new UserHolder(this, tempUsers.get(u));
                userList[u] = tempUser.getName();
                userDirectory[u] = tempUser.getUserDir();
            } else {
                userList[u] = getResources().getString(R.string.unknown_user);
                userDirectory[u] = null;
            }
            userImageIDs[u] = R.drawable.seizuretype_unknown;
        }
        int selectedUser = (unassignedDataExist ? userList.length-1 : 0);
        ImageTextSpinnerAdapter userAdapter = new ImageTextSpinnerAdapter(this, userList, userImageIDs, false);




        //initialize content of seizure entry
        callerID = getIntent().getStringExtra("callingActivityID");
//        Toast.makeText(this,"Caller ID is: "+callerID, Toast.LENGTH_LONG).show();
        if(callerID.equals(DiaryActivity.CALLING_ACTIVITY_ID)) {
            Log.d(TAG, "onCreate: Called by "+DiaryActivity.CALLING_ACTIVITY_ID);
            init_user_string = getIntent().getStringExtra(USERSTRING_EXTRA_ID);
            if(!init_user_string.equals(UserEntryHelper.UserEntryFillString)) {
                UserHolder tempUser = new UserHolder(this, init_user_string);
                File tempUserDir = tempUser.getUserDir();
                for(int u=0; u<userCount; u++) if(tempUserDir.equals(userDirectory[u])) { selectedUser = u; break; }
            }
            init_seizure_entry_string = getIntent().getStringExtra(SEIZURESTRING_EXTRA_ID);
            seizureEntry = new SeizureHolder(this, init_seizure_entry_string);
            int[] checkedWarnings = seizureEntry.getSeizureCheckedWarnings();
            for (int checkedWarning : checkedWarnings) warningChecks[checkedWarning] = true;
            int[] checkedTriggers = seizureEntry.getSeizureCheckedTrigger();
            for (int checkedTrigger : checkedTriggers) triggerChecks[checkedTrigger] = true;
            tr_button_newentry.setVisibility(View.INVISIBLE);
            tr_button_diary.setVisibility(View.VISIBLE);
            runTimer = false;
        } else {
            seizureEntry = new SeizureHolder(this, SeizureEntryHelper.SeizureEntryFillString);
            Calendar calendar = Calendar.getInstance();
            seizureEntry.setSeizureDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            seizureEntry.setSeizureTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            seizureEntry.setSeizureDuration(0L);
            seizureEntry.setSeizureEmergency(0);
            seizureEntry.setSeizureActivity(0);
            seizureEntry.setSeizureNotes(getResources().getString(R.string.ns_notes));
            tr_button_newentry.setVisibility(View.VISIBLE);
            tr_button_diary.setVisibility(View.INVISIBLE);
            runTimer = true;
        }
        seizurePeriodTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                while(runTimer) {
                    seizurePeriod_EndTime = System.currentTimeMillis();
                    long duration = (seizurePeriod_EndTime - seizurePeriod_StartTime) / 1000L;
                    seizureEntry.setSeizureDuration(duration);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            duration_button.setText(String.format("%s\n%s",
                                    SeizureEntryHelper.duration2string(seizureEntry.getSeizureDuration(),false),
                                    getResources().getString(R.string.elapsed_time_press_for_stop)));
                        }
                    });
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        });

        //get references
        userSpinner = (Spinner) findViewById(R.id.row_name_edit);
        date_button = (Button) findViewById(R.id.row_date_edit);
        time_button = (Button) findViewById(R.id.row_time_edit);
        duration_button = (Button) findViewById(R.id.row_duration_edit);
        warning_button = (Button) findViewById(R.id.row_warnings_edit);
        warning_button = (Button) findViewById(R.id.row_warnings_edit);
        trigger_button = (Button) findViewById(R.id.row_trigger_edit);
        notes_button = (Button) findViewById(R.id.row_notes_edit);
        datePicker = (DatePicker) findViewById(R.id.date_picker);
        timePicker = (TimePicker) findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        hoursPicker = (NumberPicker) findViewById(R.id.number_picker_hours);
        minutesPicker = (NumberPicker) findViewById(R.id.number_picker_minutes);
        secondsPicker = (NumberPicker) findViewById(R.id.number_picker_seconds);
        seizureTypePicker = (Spinner) findViewById(R.id.row_seizure_edit);
        seizureTypePicker.setAdapter(seizureTypeAdapter);
        seizureTypePicker.setOnItemSelectedListener(new ImageTextSpinnerSelectionListener());
        CheckBox seizureIsStatus = (CheckBox) findViewById(R.id.row_seizure_status);
        seizureIsStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seizureEntry.setSeizureIsStatus(isChecked); } });
        ListView warningPicker = (ListView) findViewById(R.id.ns_view_warning_group);
        CheckBoxGroupListener warningListener = new CheckBoxGroupListener(2);
        ListView triggerPicker = (ListView) findViewById(R.id.ns_view_trigger_group);
        CheckBoxGroupListener triggerListener = new CheckBoxGroupListener(1);
        emergencyPicker = (Spinner) findViewById(R.id.row_emergency_edit);
        activityPicker = (Spinner) findViewById(R.id.row_activity_edit);
        notesPicker = (EditText) findViewById(R.id.ns_view_notes_edit);

        //fill views with content
        userSpinner.setAdapter(userAdapter);
        userSpinner.setSelection(selectedUser);
        date_button.setText(SeizureEntryHelper.date2string(seizureEntry.getSeizureDate(), null, currentLocale, this));
        time_button.setText(SeizureEntryHelper.time2string(seizureEntry.getSeizureTime(), this));
        long duration = seizureEntry.getSeizureDuration();
        duration_button.setText(SeizureEntryHelper.duration2string(duration, false));
        setDuration(duration);
        seizureTypePicker.setSelection(seizureEntry.getSeizureTypeID());
        seizureIsStatus.setChecked(seizureEntry.isSeizureStatus());
        warningAdapter = new CheckBoxGroupAdapter(this,
                getCheckBoxes(getResources().getStringArray(R.array.ns_warning_list), warningChecks));
        warningPicker.setAdapter(warningAdapter);
        warningPicker.setOnItemClickListener(warningListener);
        String warning_string = seizureEntry.getWarnings(this, false);
        if(warning_string.length()<2) warning_string = getResources().getString(R.string.ns_not_set);
        if(warning_string.length()>maxStringLength) warning_string = warning_string.substring(0,maxStringLength-3) + "...";
        warning_button.setText(warning_string);
        triggerAdapter = new CheckBoxGroupAdapter(this,
                getCheckBoxes(getResources().getStringArray(R.array.ns_trigger_list), triggerChecks));
        triggerPicker.setAdapter(triggerAdapter);
        triggerPicker.setOnItemClickListener(triggerListener);
        String trigger_string = seizureEntry.getTriggers(this, false);
        if(trigger_string.length()<2) trigger_string = getResources().getString(R.string.ns_not_set);
        if(trigger_string.length()>maxStringLength) trigger_string = trigger_string.substring(0,maxStringLength-3) + "...";
        trigger_button.setText(trigger_string);
        emergencyPicker.setSelection(seizureEntry.getSeizureEmergency());
        activityPicker.setSelection(seizureEntry.getSeizureActivity());
        String notes_string = seizureEntry.getSeizureNotes();
        if(notes_string.length()>maxStringLength) notes_string = notes_string.substring(0,maxStringLength-3) + "...";
        notes_button.setText(notes_string);

        //start thread at the end of initialisation
        seizurePeriodTimer.start();
    }




    /**********************************************************************************************
     **** EDIT SEIZURE ENTRY **********************************************************************
     **********************************************************************************************/
    public void invokeButtonInput(@NonNull View view) {
        switch (view.getId()) {
            case R.id.row_date_edit:
                View dateView = findViewById(R.id.ns_view_date_picker);
                dateView.setVisibility(View.VISIBLE);
                subInterfaceID = R.id.view_date_edit;
                break;
            case R.id.row_time_edit:
                View timeView = findViewById(R.id.ns_view_time_picker);
                timeView.setVisibility(View.VISIBLE);
                if(Build.VERSION.SDK_INT>22) {
                    int[] sourceTime = seizureEntry.getSeizureTime();
                    timePicker.setHour(sourceTime[0]);
                    timePicker.setMinute(sourceTime[1]);
                }
                if(Build.VERSION.SDK_INT>25) timePicker.restoreDefaultFocus();
                subInterfaceID = R.id.view_time_edit;
                break;
            case R.id.row_duration_edit:
                View durationView = findViewById(R.id.ns_view_duration_picker);
                runTimer = false;
                long duration = seizureEntry.getSeizureDuration();
                int second = (int) (duration % 60L);
                secondsPicker.setValue(second);
                int minute = (int) ((duration/60L) % 60L);
                minutesPicker.setValue(minute);
                int hour = (int) (duration/3600L);
                hoursPicker.setValue(hour);
                durationView.setVisibility(View.VISIBLE);
                subInterfaceID = R.id.view_duration_edit;
                break;
            case R.id.row_warnings_edit:
                View warningView = findViewById(R.id.ns_view_warning_picker);
                warningView.setVisibility(View.VISIBLE);
                subInterfaceID = R.id.ns_view_warning_edit;
                break;
            case R.id.row_trigger_edit:
                View triggerView = findViewById(R.id.ns_view_trigger_picker);
                triggerView.setVisibility(View.VISIBLE);
                subInterfaceID = R.id.ns_view_trigger_edit;
                break;
            case R.id.row_notes_edit:
                View notesView = findViewById(R.id.ns_view_notes_picker);
                notesView.setVisibility(View.VISIBLE);
                EditText notesEdit = (EditText) findViewById(R.id.ns_view_notes_edit);
                notesEdit.setText(seizureEntry.getSeizureNotes());
                imm.showSoftInput(notesPicker, 0);
                subInterfaceID = R.id.ns_view_notes_ok;
                break;
            default:
                break;
        }
    }
    public void retrieveButtonInput(@NonNull View view) {
        switch (view.getId()) {
            case R.id.view_date_edit:
                seizureEntry.setSeizureDate(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                date_button.setText(SeizureEntryHelper.date2string(seizureEntry.getSeizureDate(), null, currentLocale, this));
                View dateView = findViewById(R.id.ns_view_date_picker);
                dateView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.view_time_edit:
                seizureEntry.setSeizureTime(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                time_button.setText(SeizureEntryHelper.time2string(seizureEntry.getSeizureTime(), this));
                View timeView = findViewById(R.id.ns_view_time_picker);
                timeView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.view_duration_edit:
                seizureEntry.setSeizureDuration(getDuration());
                duration_button.setText(SeizureEntryHelper.duration2string(seizureEntry.getSeizureDuration(), false));
                View durationView = findViewById(R.id.ns_view_duration_picker);
                durationView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_warning_edit:
                seizureEntry.setSeizureWarnings(warningAdapter);
                String warning_string = seizureEntry.getWarnings(this, false);
                if(warning_string.length()>maxStringLength)
                    warning_string = warning_string.substring(0,maxStringLength-3) + "...";
                warning_button.setText(warning_string);
                View warningView = findViewById(R.id.ns_view_warning_picker);
                warningView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_trigger_edit:
                seizureEntry.setSeizureTrigger(triggerAdapter);
                String trigger_string = seizureEntry.getTriggers(this, false);
                if(trigger_string.length()>maxStringLength)
                    trigger_string = trigger_string.substring(0,maxStringLength-3) + "...";
                trigger_button.setText(trigger_string);
                View triggerView = findViewById(R.id.ns_view_trigger_picker);
                triggerView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_notes_ok:
                seizureEntry.setSeizureNotes(notesPicker.getText().toString());
                String notesString = seizureEntry.getSeizureNotes();
                notesPicker.setText(notesString);
                if(notesString.length()>maxStringLength)
                    notesString = notesString.substring(0,maxStringLength-3) + "...";
                notes_button.setText(notesString);
                View notesView = findViewById(R.id.ns_view_notes_picker);
                notesView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            default:
                break;
        }
    }
    private void retrieveBackButtonInput(@NonNull View view) {
        switch (view.getId()) {
            case R.id.view_date_edit:
                View dateView = findViewById(R.id.ns_view_date_picker);
                dateView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.view_time_edit:
                View timeView = findViewById(R.id.ns_view_time_picker);
                timeView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.view_duration_edit:
                View durationView = findViewById(R.id.ns_view_duration_picker);
                durationView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_warning_edit:
                View warningView = findViewById(R.id.ns_view_warning_picker);
                warningView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_trigger_edit:
                View triggerView = findViewById(R.id.ns_view_trigger_picker);
                triggerView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.ns_view_notes_ok:
                View notesView = findViewById(R.id.ns_view_notes_picker);
                notesView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                Log.d(TAG, "retrieveBackButtonInput: Close notes_picker.");
                break;
            default:
                break;
        }
    }





    /**********************************************************************************************
     **** SAVE/DELETE SEIZURE ENTRY ***************************************************************
     **********************************************************************************************/
    public void saveSeizureEntry(View view) {
        if( !PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
            return;
        }

        //before saving
        //get new information for seizureType, emergency and daytime
        //because no OnItemClickListener get set to the spinner, and no other way exists for noticing changes
        seizureEntry.setSeizureTypeID(seizureTypePicker.getSelectedItemPosition());
        seizureEntry.setSeizureEmergency(emergencyPicker.getSelectedItemPosition());
        seizureEntry.setSeizureActivity(activityPicker.getSelectedItemPosition());

        //check, if all tags are filled
        Calendar calendar = Calendar.getInstance();
        int[] seizureStartDate = seizureEntry.getSeizureDate();
        if(seizureStartDate[0]*10000+seizureStartDate[1]*100+seizureStartDate[2] >
                calendar.get(Calendar.YEAR)*10000+calendar.get(Calendar.MONTH)*100+calendar.get(Calendar.DAY_OF_MONTH)) {
            Toast.makeText(this,getResources().getString(R.string.ns_error_future_date), Toast.LENGTH_SHORT).show();
            return;
        }
        if(seizureEntry.getSeizureCheckedWarnings().length==0) {
            Toast.makeText(this, getResources().getString(R.string.ns_error_no_checked_warning), Toast.LENGTH_SHORT).show();
            return;
        }
        if(seizureEntry.getSeizureCheckedTrigger().length==0) {
            Toast.makeText(this, getResources().getString(R.string.ns_error_no_checked_trigger), Toast.LENGTH_SHORT).show();
            return;
        }

        //save input taken in this activity...
        //delete old entry
        if(callerID.equals("DIARY_ACTIVITY")) {
            UserHolder _old_user = new UserHolder(this, init_user_string);
            String _old_user_name = (init_user_string.equals(UserEntryHelper.UserEntryFillString) ? null : _old_user.getUserDir().getName());
            Log.d(TAG, "saveSeizureEntry: DELETE FROM USER <"+(_old_user_name!=null ? _old_user_name : "null")+">");
            SeizureHolder _old_seizure = new SeizureHolder(this, init_seizure_entry_string);
            int[] initDate = _old_seizure.getSeizureDate();
            ArrayList<String> _old_data = SeizureEntryHelper.loadSeizureList(this, _old_user_name, initDate[0], initDate[1]);
            for(int od=_old_data.size()-1; od>=0; od--) {
                String old_content = _old_data.get(od);
                if(old_content.equals(init_seizure_entry_string)) _old_data.remove(od);
            }
            boolean worked = SeizureEntryHelper.saveSeizureList(this, _old_user_name, initDate[0], initDate[1], _old_data);
            if(!worked) {
                Toast.makeText(this, getResources().getString(R.string.ns_error_save_new_entry), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        //save new entry
        int[] newDate = seizureEntry.getSeizureDate();
        int newUser = userSpinner.getSelectedItemPosition();
        String newUserName = (userList[newUser].equals(getResources().getString(R.string.unknown_user))
                ? null : userDirectory[newUser].getName());
        Log.d(TAG, "saveSeizureEntry: INSERT TO USER <"+(newUserName!=null ? newUserName : "null")+">");
        ArrayList<String> new_seizure_list = SeizureEntryHelper.loadSeizureList(this, newUserName, newDate[0], newDate[1]);
        String newSeizureEntryString = seizureEntry.createSeizureEntryString(this, true);
        new_seizure_list.add(newSeizureEntryString);
        Collections.sort(new_seizure_list);
        boolean hasSaved = SeizureEntryHelper.saveSeizureList(this, newUserName, newDate[0], newDate[1], new_seizure_list);
        if(!hasSaved) {
            Toast.makeText(this, getResources().getString(R.string.ns_error_save_new_entry), Toast.LENGTH_SHORT).show();
            return;
        }

        System.out.println("New Seizure entry:\n"+newSeizureEntryString);

        finish();
    }
    public void deleteSeizureEntry(View view) {
        if( !PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
            return;
        }

        //delete entry
        if(callerID.equals("DIARY_ACTIVITY")) {
            UserHolder _old_user = new UserHolder(this, init_user_string);
            String _old_user_name = (init_user_string.equals(UserEntryHelper.UserEntryFillString) ? null : _old_user.getUserDir().getName());
            SeizureHolder _old_seizure = new SeizureHolder(this, init_seizure_entry_string);
            int[] initDate = _old_seizure.getSeizureDate();
            ArrayList<String> _old_data = SeizureEntryHelper.loadSeizureList(this, _old_user_name, initDate[0], initDate[1]);
            for(int od=_old_data.size()-1; od>=0; od--) {
                String old_content = _old_data.get(od);
                if(old_content.equals(init_seizure_entry_string)) _old_data.remove(od);
            }
            boolean worked = SeizureEntryHelper.saveSeizureList(this, _old_user_name, initDate[0], initDate[1], _old_data);
            if(!worked) {
                Toast.makeText(this, getResources().getString(R.string.ns_error_save_new_entry), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        finish();
    }




    /**********************************************************************************************
     **** HELPER FUNCTIONS ************************************************************************
     **********************************************************************************************/
    @Override
    public void onBackPressed() {
        if(subInterfaceID!=Integer.MAX_VALUE) {
            retrieveBackButtonInput(findViewById(subInterfaceID));
        } else {
            runTimer = false;
            try {
                seizurePeriodTimer.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            finish();
        }
//        super.onBackPressed();
    }
    private ArrayList<CheckBoxGroupItem> getCheckBoxes(@NonNull String[] entries, @Nullable boolean[] entriesChecked){
        ArrayList<CheckBoxGroupItem> list = new ArrayList<>();
        int listLength = entries.length;
        boolean[] selection = new boolean[listLength];
        if(entriesChecked != null) {
            int checkLength = entriesChecked.length;
            for(int e=0; e<listLength; e++) {
                selection[e] = (e < checkLength && entriesChecked[e]);
            }
        }
        for(int e=0; e<listLength; e++){
            String entry = entries[e];
            CheckBoxGroupItem item = new CheckBoxGroupItem();
            item.setChecked(selection[e]);
            item.setText(entry);
            list.add(item);
        }
        return list;
    }
    private void setDuration(long _dur) {
        int durationHours = (int) (_dur/3600L);
        int durationMinutes = (int) ((_dur/60L) % 60L);
        int durationSeconds = (int) (_dur % 60L);
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(Math.max(48,durationHours));
        hoursPicker.setValue(durationHours);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(60);
        minutesPicker.setValue(durationMinutes);
        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(60);
        secondsPicker.setValue(durationSeconds);
    }
    private long getDuration() {
        return (hoursPicker.getValue()*3600L)+(minutesPicker.getValue()*60L)+(long)secondsPicker.getValue();
    }
}
