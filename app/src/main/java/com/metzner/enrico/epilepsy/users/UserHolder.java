package com.metzner.enrico.epilepsy.users;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;

import java.io.File;
import java.util.Calendar;

public class UserHolder {

    private static final String TAG = "USER_HOLDER";

    private String userEntryString;
    private File userDirectory;
    private String name;
    private int colour;
    private int colourIndex;
    private int genderType;
    private int[] birthday;
    private String typeOfEpilepsy;
    private String medication;

    private int[] colourArray;

    //constructor
    public UserHolder(@NonNull Context context, @NonNull String _userEntryString) {
        userEntryString = _userEntryString;
        Log.d(TAG, "UserHolder: "+userEntryString);
        String typeOfEpilepsy_unknown = context.getResources().getString(R.string.nu_not_available);
        colourArray = context.getResources().getIntArray(R.array.colors_pie_chart);
        if( _userEntryString.equals(UserEntryHelper.UserEntryFillString) ) {
            userDirectory = null;
            name = "";
            colour = colourArray[5];
            colourIndex = 5;
            genderType = 0;
            Calendar cal = Calendar.getInstance();
            birthday = new int[]{cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH) };
            typeOfEpilepsy = typeOfEpilepsy_unknown;
            medication = typeOfEpilepsy_unknown;
        } else {
            String[] encryptParts = _userEntryString.split(",");
            userDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "Epilepsy/"+encryptParts[0]);
            name = UserEntryHelper.replaceKommata(encryptParts[1], true);
            colour = Color.parseColor(encryptParts[2]);
            colourIndex = 0;
            int colourLength = colourArray.length;
            for(int ci=0; ci<colourLength; ci++) {
                if(colour == colourArray[ci]) {
                    colourIndex = ci;
                    break;
                }
            }
            genderType = Integer.parseInt(encryptParts[3]);
            int temp_birthday = Integer.parseInt(encryptParts[4]);
            birthday = new int[]{temp_birthday/10000, (temp_birthday/100)%100-1, temp_birthday%100};
            typeOfEpilepsy = UserEntryHelper.replaceKommata(encryptParts[5], true);
            medication = (encryptParts.length>6 ? UserEntryHelper.replaceKommata(encryptParts[6], true) : typeOfEpilepsy_unknown);
        }
    }

    //getter and setter
    public String getUserEntryString() { return userEntryString; }
    public File getUserDir() { return userDirectory; }
    public void setUserDir(String user_dir_name) {
        this.userDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Epilepsy/"+user_dir_name);
    }
    public String getName() { return name; }
    public void setName(String _name) { name = _name; }
    public int getColour() { return colour; }
    public int getColourID() { return colourIndex; }
    public void setColour(int _colourID) { colour = colourArray[_colourID]; colourIndex = _colourID; }
    public int getGender() { return genderType; }
    public void setGender(int _gender) { genderType = _gender; }
    public int[] getBirthday() { return birthday; }
    public void setBirthday(int birthdayYear, int birthdayMonth, int birthdayDayOfMonth) {
        birthday = new int[]{birthdayYear, birthdayMonth, birthdayDayOfMonth};
    }
    public String getTypeOfEpilepsy() { return typeOfEpilepsy; }
    public void setTypeOfEpilepsy(String _typeOfEpilepsy) { typeOfEpilepsy = _typeOfEpilepsy; }
    public String getMedication() { return medication; }
    public void setMedication(String _medication) { medication = _medication; }

    //create user entry string
    public String createSeizureEntryString(Context context, boolean override) {
        StringBuilder newUserEntryString = new StringBuilder();

        //directory == userID
        newUserEntryString.append(userDirectory.getName());

        //name
        newUserEntryString.append(",").append(UserEntryHelper.replaceKommata(name, false));

        //colour
        newUserEntryString.append(",").append(String.format("#%X", colour));

        //gender
        newUserEntryString.append(",").append(genderType);

        //birthday
        newUserEntryString.append(",").append(birthday[0]*10000 + (birthday[1]+1)*100 + birthday[2]);

        //type of epilepsy
        newUserEntryString.append(",").append(UserEntryHelper.replaceKommata(typeOfEpilepsy, false));

        //medication
        newUserEntryString.append(",").append(UserEntryHelper.replaceKommata(medication, false));


        if(override)
            userEntryString = newUserEntryString.toString();

        return newUserEntryString.toString();
    }
}
