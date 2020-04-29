package com.metzner.enrico.epilepsy.seizure;

import android.content.Context;
import android.support.annotation.NonNull;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.drop_down_menus.CheckBoxGroupAdapter;
import com.metzner.enrico.epilepsy.drop_down_menus.CheckBoxGroupItem;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;

@SuppressWarnings("CanBeFinal")
public class SeizureHolder {
    private String seizureEntryString;
    private int[] seizureDate;
    private int[] seizureTime;
    private long seizureDuration;
    private int seizureTypeID;
    private boolean seizureIsStatus;
    private int[] seizureCheckedWarnings;
    private int[] seizureCheckedTrigger;
    private int seizureEmergencyID;
    private int seizureActivityID;
    private String seizure_notes;

    // constructor
    public SeizureHolder(@NonNull Context context, @NonNull String _seizure_entry) {
        int pos = _seizure_entry.indexOf("{");
        if (pos>0) {
            seizureEntryString = _seizure_entry;
            String entry_without_comment = _seizure_entry.substring(0, pos);
            String[] info = entry_without_comment.split(",");
            int date_combined = Integer.parseInt(info[0]);
            int date_year = date_combined / 10000;
            int date_month = (date_combined-10000*date_year) / 100;
            int date_day = date_combined % 100;
            seizureDate = new int[]{date_year, date_month-1, date_day};
            int time_combined = Integer.parseInt(info[1]);
            int time_hour = time_combined / 100;
            int time_minute = time_combined % 100;
            seizureTime = new int[]{time_hour, time_minute};
            seizureDuration = (long) Integer.parseInt(info[2]);
            int temp_seizureTypeID = Integer.parseInt(info[3]);
            seizureTypeID = Math.abs(temp_seizureTypeID);
            seizureIsStatus = (temp_seizureTypeID<0);
            String[] warnings = (info[4].length()>0 ? info[4].split("#") : new String[0]);
            seizureCheckedWarnings = new int[warnings.length];
            for(int w=0; w<warnings.length; w++) seizureCheckedWarnings[w] = Integer.parseInt(warnings[w]);
            String[] trigger = (info[5].length()>0 ? info[5].split("#") : new String[0]);
            seizureCheckedTrigger = new int[trigger.length];
            for(int w=0; w<trigger.length; w++) seizureCheckedTrigger[w] = Integer.parseInt(trigger[w]);
            seizureEmergencyID = Integer.parseInt(info[6]);
            seizureActivityID = 0;
            int[] activityIDs = context.getResources().getIntArray(R.array.ns_activity_list_ids);
            int sourceActivityID = Integer.parseInt(info[7]);
            for(int at=0; at<activityIDs.length; at++) {
                if(activityIDs[at]==sourceActivityID) {
                    seizureActivityID = at;
                    break;
                }
            }
            seizure_notes = _seizure_entry.substring(pos + 1, _seizure_entry.length() - 1);

//            Toast.makeText(context,
//                    seizureEntryString+"\n"+
//                            "type: "+seizureTypeID+"\n"+
//                            "warnings: "+getWarnings(context, true)+"\n"+
//                            "trigger: "+getTriggers(context, true)+"\n"+
//                            seizure_notes
//                    , Toast.LENGTH_LONG).show();
        } else {
            seizureEntryString = SeizureEntryHelper.SeizureEntryFillString;
            seizureDate = new int[]{2000,0,1};
            seizureTime = new int[]{12, 0};
            seizureDuration = 0L;
            seizureTypeID = 0;
            seizureIsStatus = false;
            seizureCheckedWarnings = new int[0];
            seizureCheckedTrigger = new int[0];
            seizureEmergencyID = 0;
            seizureActivityID = 0;
            seizure_notes = "";
        }
    }

    //getter and setter
    public String getSeizureEntryString() { return seizureEntryString; }
    public int[] getSeizureDate() { return seizureDate; }
    public void setSeizureDate(int _year, int _month, int _day_of_month) {
        seizureDate[0] = _year;
        seizureDate[1] = _month;
        seizureDate[2] = _day_of_month;
    }
    public int[] getSeizureTime() { return seizureTime; }
    public void setSeizureTime(int _hour, int _minute) {
        seizureTime[0] = _hour;
        seizureTime[1] = _minute;
    }
    public long getSeizureDuration() { return seizureDuration; }
    public void setSeizureDuration(long _duration) {
        seizureDuration = _duration;
    }
    public void setSeizureTypeID(int _seizure_type_id) { seizureTypeID = _seizure_type_id; }
    public int getSeizureTypeID() { return seizureTypeID; }
    public void setSeizureIsStatus(boolean _status) { seizureIsStatus = _status; }
    public boolean isSeizureStatus() { return seizureIsStatus; }
    public String getWarnings(Context context, boolean asNumbers) {
        return sublist_of_stringArrayResource(seizureCheckedWarnings, context, R.array.ns_warning_list, asNumbers);
    }
    public void setSeizureWarnings(@NonNull CheckBoxGroupAdapter _warning_adapter) {
        seizureCheckedWarnings = get_checkedItemList_from_adapter(_warning_adapter);
    }
    public int[] getSeizureCheckedWarnings() { return seizureCheckedWarnings; }
    public void setSeizureTrigger(@NonNull CheckBoxGroupAdapter _trigger_adapter) {
        seizureCheckedTrigger = get_checkedItemList_from_adapter(_trigger_adapter);
    }
    public String getTriggers(Context context, boolean asNumbers) {
        return sublist_of_stringArrayResource(seizureCheckedTrigger, context, R.array.ns_trigger_list, asNumbers);
    }
    public int[] getSeizureCheckedTrigger() { return seizureCheckedTrigger; }
    public void setSeizureEmergency(int _emergency_position) { seizureEmergencyID = _emergency_position; }
    public int getSeizureEmergency() { return seizureEmergencyID; }
    public void setSeizureActivity(int _activity_position) { seizureActivityID = _activity_position; }
    public int getSeizureActivity() { return seizureActivityID; }
    public String getSeizureNotes() { return seizure_notes; }
    public void setSeizureNotes(String _notes) {
        seizure_notes = _notes;
        seizure_notes = seizure_notes.replace('{', '(').replace('}',')');
        seizure_notes = seizure_notes.replace("\n", " ");
    }

    //create seizure entry string
    public String createSeizureEntryString(Context context, boolean override) {
        String newSeizureEntryString = "";

        //date
        newSeizureEntryString += seizureDate[0] +
                (seizureDate[1]>8 ? "" : "0") + (seizureDate[1]+1) +
                (seizureDate[2]>9 ? "" : "0") + seizureDate[2];
        //time
        newSeizureEntryString += "," + (seizureTime[0]>9 ? "" : "0") + seizureTime[0] +
                (seizureTime[1]>9 ? "" : "0") + seizureTime[1];
        //duration
        newSeizureEntryString += "," + seizureDuration;
        //seizureTypeID *= (seizureIsStatus.isChecked() ? -1 : 1);
        newSeizureEntryString += "," + (seizureTypeID*(seizureIsStatus?-1:1));
        //warnings
        newSeizureEntryString += "," + getWarnings(context, true);
        //triggers
        newSeizureEntryString += "," + getTriggers(context, true);
        //emergency
        newSeizureEntryString += "," + seizureEmergencyID;
        //daytime
        int[] activityIDs = context.getResources().getIntArray(R.array.ns_activity_list_ids);
        newSeizureEntryString += "," + activityIDs[seizureActivityID];
        //notes
        String notes_string = seizure_notes;
        if( seizure_notes.equals(context.getResources().getString(R.string.ns_notes))
                || seizure_notes.length()==0 ) {
            notes_string = "no comment";
        }
        newSeizureEntryString += ",{" + notes_string + "}";

        if(override)
            seizureEntryString = newSeizureEntryString;

        return newSeizureEntryString;
    }

    private int[] get_checkedItemList_from_adapter(@NonNull CheckBoxGroupAdapter _adapter) {
        int checkedItemsCount = 0;
        int cbLength = _adapter.getCount();
        for(int br = 0; br<cbLength; br++) {
            CheckBoxGroupItem b = (CheckBoxGroupItem) _adapter.getItem(br);
            if(b.isChecked()) checkedItemsCount++;
        }
        int[] checkedItemList = new int[checkedItemsCount];
        int checkedItemsPosition = 0;
        for(int br = 0; br<cbLength; br++) {
            CheckBoxGroupItem b = (CheckBoxGroupItem) _adapter.getItem(br);
            if(b.isChecked()) {
                checkedItemList[checkedItemsPosition] = br;
                checkedItemsPosition++;
            }
        }
        return checkedItemList;
    }
    private String sublist_of_stringArrayResource(@NonNull int[] ids, Context context, int stringArrayID, boolean asNumbers) {
        String contentString;
        StringBuilder contentStringBuilder = new StringBuilder();
        int cbLength = ids.length;
        if (cbLength > 0) {
            if (asNumbers) {
                contentStringBuilder.append(ids[0]);
            } else {
                contentStringBuilder.append(context.getResources().getStringArray(stringArrayID)[ids[0]]);
            }
            if (cbLength > 1) {
                for (int cb = 1; cb < cbLength; cb++) {
                    if(asNumbers) {
                        contentStringBuilder.append('#');
                        contentStringBuilder.append(ids[cb]);
                    } else {
                        contentStringBuilder.append(", ");
                        contentStringBuilder.append(context.getResources().getStringArray(stringArrayID)[ids[cb]]);
                    }
                }
            }
        }
        contentString = contentStringBuilder.toString();
        return contentString;
    }
}
