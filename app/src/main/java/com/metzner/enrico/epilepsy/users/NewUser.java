package com.metzner.enrico.epilepsy.users;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.CryptoHelper;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.settings.LocaleManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class NewUser extends AppCompatActivity {

    private static final int maxStringLength = 30;
    private static final String TAG = "NEW_USER_ACTIVITY";
    public static final String ENTRYSTATE_EXTRA_ID = "entryStateID";
    public static final String ENTRYSTATE_NEW = "new";
    public static final String ENTRYSTATE_EDIT = "edit";
    public static final String ENTRYSTRING_EXTRA_ID = "userEntryString";

    private Button nameButton,
                   birthdayButton,
                   epilepsyTypeButton,
                   medicationButton;
    private Spinner colourView,
                    genderView;
    private EditText textEdit;
    private Locale currentLocale;

    private String entryStateID;
    private UserHolder userEntry;
    private int textInterfaceID = Integer.MAX_VALUE;
    private int subInterfaceID = Integer.MAX_VALUE;
    private String[] months;
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
        setContentView(R.layout.activity_new_user);
        currentLocale = getResources().getConfiguration().locale;

        months = getResources().getStringArray(R.array.monthAbbr);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //initialize content of seizure entry
        entryStateID = getIntent().getStringExtra(ENTRYSTATE_EXTRA_ID);
        String init_user_entry_string = UserEntryHelper.UserEntryFillString;
        if(entryStateID.equals(ENTRYSTATE_EDIT)) {
            init_user_entry_string = getIntent().getStringExtra(ENTRYSTRING_EXTRA_ID);
            userEntry = new UserHolder(this, init_user_entry_string);
            View creationView = findViewById(R.id.nu_buttons_new);
            creationView.setVisibility(View.INVISIBLE);
            View editingView = findViewById(R.id.nu_buttons_edit);
            editingView.setVisibility(View.VISIBLE);
        } else {
            userEntry = new UserHolder(this, init_user_entry_string);
            userEntry.setUserDir(UserEntryHelper.newUserDirectory(this));
        }

        //get references
        TextView idView = (TextView) findViewById(R.id.nu_row_userID_edit);
        nameButton = (Button) findViewById(R.id.nu_row_name_edit);
        colourView = (Spinner) findViewById(R.id.nu_row_colour_edit);
        genderView = (Spinner) findViewById(R.id.nu_row_gender_edit);
        birthdayButton = (Button) findViewById(R.id.nu_row_birthday_edit);
        epilepsyTypeButton = (Button) findViewById(R.id.nu_row_epi_type_edit);
        medicationButton = (Button) findViewById(R.id.nu_row_medication_edit);
        textEdit = (EditText) findViewById(R.id.nu_view_text_edit);
        NumberPicker
                dayPicker = (NumberPicker) findViewById(R.id.birthday_picker_day),
                monthPicker = (NumberPicker) findViewById(R.id.birthday_picker_month),
                yearPicker = (NumberPicker) findViewById(R.id.birthday_picker_year);


        //fill in data
        Calendar calendar = Calendar.getInstance();
        dayPicker.setMaxValue(30); dayPicker.setValue(0);
        dayPicker.setFormatter(new NumberPicker.Formatter() {
            @Override public String format(int value) { return ""+(value+1); }});
        monthPicker.setMaxValue(11); monthPicker.setValue(0);
        monthPicker.setFormatter(new NumberPicker.Formatter() {
            @Override public String format(int value) { return months[value]; } });
        yearPicker.setMaxValue(calendar.get(Calendar.YEAR)-1900); yearPicker.setValue(100);
        yearPicker.setFormatter(new NumberPicker.Formatter() {
            @Override public String format(int value) { return ""+(1900+value); } });
        idView.setText(userEntry.getUserDir().getName());
        if( ! userEntry.getUserEntryString().equals(UserEntryHelper.UserEntryFillString) ) {
            nameButton.setText(userEntry.getName());
            colourView.setSelection(userEntry.getColourID());
            birthdayButton.setText(SeizureEntryHelper.date2string(userEntry.getBirthday(), null, currentLocale, this));
            int[] birthdayValues = userEntry.getBirthday();
            dayPicker.setValue(birthdayValues[2]-1);
            monthPicker.setValue(birthdayValues[1]);
            yearPicker.setValue(birthdayValues[0]-1900);
            genderView.setSelection(userEntry.getGender());
            epilepsyTypeButton.setText(userEntry.getTypeOfEpilepsy());
            medicationButton.setText(userEntry.getMedication());
        }
    }




    /**********************************************************************************************
     **** EDIT USER ENTRY *************************************************************************
     **********************************************************************************************/
    public void invokeButtonInput(View view) {
        switch (view.getId()) {
            case R.id.nu_row_name_edit:
                View nameView = findViewById(R.id.nu_view_text_picker);
                nameView.setVisibility(View.VISIBLE);
                String name_text = getResources().getString(R.string.nu_name);
                TextView titleView = findViewById(R.id.nu_view_text_title);
                titleView.setText(name_text);
                titleView.setContentDescription(name_text);
                EditText editView = (EditText) findViewById(R.id.nu_view_text_edit);
                editView.setContentDescription(name_text);
                if(nameButton.getText().length()>0 && nameButton.getText().toString().equals(name_text))
                    name_text = nameButton.getText().toString();
                editView.setText(name_text);
                imm.showSoftInput(editView, 0);
                textInterfaceID = view.getId();
                subInterfaceID = view.getId();
                break;
            case R.id.nu_row_birthday_edit:
                View dateView = findViewById(R.id.nu_view_birthday_picker);
                dateView.setVisibility(View.VISIBLE);
                subInterfaceID = R.id.nu_view_birthday_edit;
                break;
            case R.id.nu_row_epi_type_edit:
                View epiView = findViewById(R.id.nu_view_text_picker);
                epiView.setVisibility(View.VISIBLE);
                imm.showSoftInput(view, 0);
                String epi_text = getResources().getString(R.string.nu_epi_type);
                TextView epiTitleView = findViewById(R.id.nu_view_text_title);
                epiTitleView.setText(epi_text);
                epiTitleView.setContentDescription(epi_text);
                EditText epiEditView = (EditText) findViewById(R.id.nu_view_text_edit);
                epiEditView.setContentDescription(epi_text);
                if(epilepsyTypeButton.getText().length()>0 && epilepsyTypeButton.getText().toString().equals(epi_text))
                    epi_text = epilepsyTypeButton.getText().toString();
                epiEditView.setText(epi_text);
                textInterfaceID = view.getId();
                subInterfaceID = view.getId();
                break;
            case R.id.nu_row_medication_edit:
                View mediView = findViewById(R.id.nu_view_text_picker);
                mediView.setVisibility(View.VISIBLE);
                imm.showSoftInput(view, 0);
                String medi_text = getResources().getString(R.string.nu_medi);
                TextView mediTitleView = findViewById(R.id.nu_view_text_title);
                mediTitleView.setText(medi_text);
                mediTitleView.setContentDescription(medi_text);
                EditText mediEditView = (EditText) findViewById(R.id.nu_view_text_edit);
                mediEditView.setContentDescription(medi_text);
                if(medicationButton.getText().length()>0 && medicationButton.getText().toString().equals(medi_text))
                    medi_text = medicationButton.getText().toString();
                mediEditView.setText(userEntry.getMedication());
                textInterfaceID = view.getId();
                subInterfaceID = view.getId();
                break;
            default:
                break;
        }
    }
    public void retrieveButtonInput(View view) {
        switch (view.getId()) {
            case R.id.nu_view_text_ok:
                String inputText = trim(textEdit.getText().toString());
                switch (textInterfaceID) {
                    case R.id.nu_row_name_edit: userEntry.setName(inputText); break;
                    case R.id.nu_row_epi_type_edit: userEntry.setTypeOfEpilepsy(inputText); break;
                    case R.id.nu_row_medication_edit: userEntry.setMedication(inputText); break;
                    default: break;
                }
                View nameView = findViewById(R.id.nu_view_text_picker);
                nameView.setVisibility(View.INVISIBLE);
                if(inputText.length()>maxStringLength) inputText = inputText.substring(0,maxStringLength-3) + "...";
                Button buttonView = (Button) findViewById(textInterfaceID);
                buttonView.setText(inputText);
                textInterfaceID = Integer.MAX_VALUE;
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.nu_view_birthday_edit:
                NumberPicker yearPicker = (NumberPicker) findViewById(R.id.birthday_picker_year);
                NumberPicker monthPicker = (NumberPicker) findViewById(R.id.birthday_picker_month);
                NumberPicker dayPicker = (NumberPicker) findViewById(R.id.birthday_picker_day);
                userEntry.setBirthday(yearPicker.getValue()+1900, monthPicker.getValue(), dayPicker.getValue()+1);
                birthdayButton.setText(SeizureEntryHelper.date2string(userEntry.getBirthday(), null, currentLocale, this));
                View dateView = findViewById(R.id.nu_view_birthday_picker);
                dateView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            default:
                break;
        }
    }
    private void retrieveBackButtonInput(@NonNull View view) {
        switch (view.getId()) {
            case R.id.nu_row_name_edit:
                imm.toggleSoftInput(0,0);
                break;
            case R.id.nu_row_epi_type_edit:
                imm.toggleSoftInput(0,0);
                break;
            case R.id.nu_row_medication_edit:
                imm.toggleSoftInput(0,0);
                break;
            case R.id.nu_view_text_picker:
                view.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            case R.id.nu_view_birthday_edit:
                View dateView = findViewById(R.id.nu_view_birthday_picker);
                dateView.setVisibility(View.INVISIBLE);
                subInterfaceID = Integer.MAX_VALUE;
                break;
            default:
                break;
        }
    }




    /**********************************************************************************************
     **** SAVE/DELETE USER ENTRY ******************************************************************
     **********************************************************************************************/
    public void saveUser(View view) {
        if( !PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
            return;
        }

        //check if required data is set:
        if( nameButton.getText().equals(nameButton.getContentDescription()) ||
                nameButton.getText().length()<3 ) {
            Toast.makeText(this, "NAME", Toast.LENGTH_SHORT).show();
            return;
        }

        //collect data
        userEntry.setColour(colourView.getSelectedItemPosition());
        userEntry.setGender(genderView.getSelectedItemPosition());
        ArrayList<String> originalUserList = UserEntryHelper.loadUsers(this);
        ArrayList<String> newUserList = new ArrayList<>();
        String originalUserEntry = userEntry.getUserEntryString();
        boolean replaceOriginal = false;
        for(String ori: originalUserList) {
            if(ori.equals(originalUserEntry)) {
                newUserList.add(userEntry.createSeizureEntryString(this, true));
                replaceOriginal = true;
            } else {
                newUserList.add(ori);
            }
        }
        if(!replaceOriginal) newUserList.add(userEntry.createSeizureEntryString(this, true));
        Log.d(TAG, "saveUser: insert entry: "+userEntry.getUserEntryString());

        //save
        if( ! UserEntryHelper.saveUsers(newUserList)) {
            Toast.makeText(this, getResources().getString(R.string.nu_error_save_user_profil), Toast.LENGTH_SHORT).show();
        } else {
            if(entryStateID.equals(ENTRYSTATE_NEW)) {
                if(!userEntry.getUserDir().mkdir()) {
                    Log.d(TAG, "saveUser: Could not create new User Directory <" + userEntry.getUserDir().getAbsolutePath() + ">!");
                } else if(SeizureEntryHelper.existNotAssignedData(this)) {
                    View unassignedDataView = findViewById(R.id.nu_view_unassigned_data);
                    unassignedDataView.setVisibility(View.VISIBLE);
                } else {
                    finish();
                }
            } else {
                finish();
            }
        }
    }
    public void deleteUser(View view) {
        if( !PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            PermissionsHelper.showPermissionUsageInfo(this,
                    getResources().getString(R.string.permission_write_external_storage_description));
            return;
        }

        AlertDialog.Builder deletionAlert = new AlertDialog.Builder(this);
        deletionAlert.setMessage(R.string.delete_user_warning);
        deletionAlert.setPositiveButton(R.string.bt_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                deleteUserDataAndSeizureEntries();
            } });
        deletionAlert.setNegativeButton(R.string.bt_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            } });
        AlertDialog deletionDialog = deletionAlert.create();
        deletionDialog.show();
    }
    private void deleteUserDataAndSeizureEntries() {
        //collect data
        ArrayList<String> originalUserList = UserEntryHelper.loadUsers(this);
        ArrayList<String> newUserList = new ArrayList<>();
        String originalUserEntry = userEntry.getUserEntryString();
        boolean foundOriginal = false;
        for(String ori: originalUserList) {
            if(ori.equals(originalUserEntry)) {
                foundOriginal = true;
            } else {
                newUserList.add(ori);
            }
        }
        if(!foundOriginal)
            Toast.makeText(this, getResources().getString(R.string.nu_error_delete_user_profil), Toast.LENGTH_SHORT).show();

        //save
        if( ! UserEntryHelper.saveUsers(newUserList)) {
            Toast.makeText(this, getResources().getString(R.string.nu_error_delete_user_profil), Toast.LENGTH_SHORT).show();
        } else {
            deleteRecursivly(userEntry.getUserDir());
            finish();
        }
    }
    private void deleteRecursivly(@NonNull File _dir) {
        File[] subFiles = _dir.listFiles();
        for(File sf: subFiles) {
            if(sf.isDirectory()) {
                deleteRecursivly(sf);
            } else {
                if(!sf.delete())
                    Toast.makeText(this,
                            getResources().getString(R.string.nu_error_delete_seizure_file)+" <"+sf.getName()+">",
                            Toast.LENGTH_SHORT).show();
            }
        }
        if(!_dir.delete())
            Toast.makeText(this,
                    getResources().getString(R.string.nu_error_delete_user_directory)+" <"+_dir.getName()+">",
                    Toast.LENGTH_SHORT).show();
    }




    /**********************************************************************************************
     **** COPY UNASSIGNED SEIZURE DATA ************************************************************
     **********************************************************************************************/
    public void copyDataChoice(View view) {
        if(view.getId()==R.id.nu_view_unassigned_data_copy_no) {
            finish();
        }
        if(view.getId()==R.id.nu_view_unassigned_data_copy_yes) {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Epilepsy");
            File[] files = folder.listFiles();
            File newFolder = userEntry.getUserDir();
            for(File f: files) {
                if(!f.isDirectory() && f.getName().startsWith("data")) {
                    Log.d(TAG, "copyDataChoice: try to copy <"+f.getAbsolutePath()+">...");
                    File newFile = new File(newFolder, f.getName());
                    try (InputStream in = new FileInputStream(f)) {
                        try (OutputStream out = new FileOutputStream(newFile)) {
                            // Transfer bytes from in to out
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if(newFile.exists()) {
                            if(newFile.length()==f.length()) {
                                if(!f.delete()) {
                                    Log.d(TAG, "copyDataChoice: Could not delete directory <"+f.getAbsolutePath()+">");
                                    throw new IOException("Could not delete directory <"+f.getAbsolutePath()+">");
                                } else {
                                    Log.d(TAG, "copyFilesRecursive: delete <"+f.getAbsolutePath()+">...");
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
            finish();
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
            finish();
        }
//        super.onBackPressed();
    }
    @NonNull
    private String trim(String _in) {
        String _out = CryptoHelper.trimToAlphabet(_in);
        int leadingBlanks=0, trailingBlanks=0;
        int stringLength = _out.length();
        for(int lb=0; lb<stringLength; lb++) { if(_out.charAt(lb)==' ') { leadingBlanks++; } else { break; } }
        for(int tb=stringLength-1; tb>=0; tb--) { if(_out.charAt(tb)==' ') { trailingBlanks++; } else { break; } }
        return _out.substring(leadingBlanks, stringLength-trailingBlanks);
    }
}
