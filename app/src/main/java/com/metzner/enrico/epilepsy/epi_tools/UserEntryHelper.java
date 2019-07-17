package com.metzner.enrico.epilepsy.epi_tools;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.metzner.enrico.epilepsy.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public abstract class UserEntryHelper {

    private static final String TAG = "USER_ENTRY_HELPER";

    public static final String UserEntryFillString = "-9999";

    public static String newUserDirectory(Context context) {
        File[] checkFiles;
        ArrayList<String> existingUserDirectories = new ArrayList<>();
        if( PermissionsHelper.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            checkFiles = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                                 "Epilepsy").listFiles();
            if (checkFiles != null) {
                for (File f : checkFiles) {
                    if (f.isDirectory() && f.getName().startsWith("User"))
                        existingUserDirectories.add(f.getName());
                }
            }
        }
        Random userRandom = new Random();
        String testUser;
        while (true) {
            testUser = "User" +
                    (int) (9.9999 * userRandom.nextDouble()) + "" +
                    (int) (9.9999 * userRandom.nextDouble()) + "" +
                    (int) (9.9999 * userRandom.nextDouble()) + "" +
                    (int) (9.9999 * userRandom.nextDouble());
            boolean userExists = false;
            for (String eud : existingUserDirectories) {
                if (eud.equals(testUser)) {
                    userExists = true;
                    break;
                }
            }
            if (!userExists) break;
        } ;
        return testUser;
    }

    public static String replaceKommata(String _in, boolean backward) {
        String _out;
        if(backward) {
            _out = _in.replace("%comma%", ",");
        } else {
            _out = _in.replace(",", "%comma%");
        }
        return _out;
    }

    public static ArrayList<String> loadUsers(Context context) {
        String _file_name = "Epilepsy/users.epi";
        ArrayList<String> userLines = new ArrayList<>();
        userLines.clear();
        if( !PermissionsHelper.isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
            return userLines;

        File _openFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), _file_name);
        if(!_openFile.exists()) {
            Toast.makeText(context, context.getResources().getString(R.string.ueh_error_file_dont_exist), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "loadUsers: File <"+_openFile.getAbsolutePath()+"> does not exist.");
            return userLines;
        }
        try {
            FileInputStream fis = new FileInputStream(_openFile);
            InputStreamReader iss = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(iss);

            //get crypt key
            String firstLine = br.readLine();
            String key_firstGuess = CryptoHelper.crypt(firstLine, CryptoHelper.checkString, CryptoHelper.DECRYPT);
            Log.d(TAG, "loadUsers: evaluate key:\n"+CryptoHelper.checkString+"\n"+firstLine+"\n"+key_firstGuess);
            int keyLength = 0;
            for(int possibleKeyLength=41; possibleKeyLength<=61; possibleKeyLength+=2) {
                String key = key_firstGuess.substring(0, possibleKeyLength);
                String test = key_firstGuess.substring(possibleKeyLength,2*possibleKeyLength);
                boolean foundNoError = true;
                for (int t = 0; t<possibleKeyLength; t++) {
                    if(key.charAt(t)!=test.charAt(t)) {
                        foundNoError = false;
                        break;
                    }
                }
                if(foundNoError) {
                    keyLength = possibleKeyLength;
                    Log.d(TAG, "loadUsers: KeyLength = "+keyLength);
                    break;
                }
            }
            if(keyLength==0) {
                Log.d(TAG, "loadUsers: Error during key guess... - could not determine key-length...");
//                Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
                return userLines;
            }
            String decryptKey = key_firstGuess.substring(0,keyLength);
            Log.d(TAG, "loadUsers: key = "+decryptKey);

            while(true) {
                String line = br.readLine();
                if(line==null) break;
                userLines.add(CryptoHelper.crypt(line, decryptKey, CryptoHelper.DECRYPT));
            }



            fis.close();
            iss.close();
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return userLines;
    }
    public static boolean saveUsers(@NonNull ArrayList<String> entries) {
        String _file_name = "Epilepsy/users.epi";
        File _saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), _file_name);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(_saveFile));
            int numberOfEntries = entries.size();
            //first enter crypt-key-form
            String newKey = CryptoHelper.createNewKey();
            bw.write(CryptoHelper.crypt(CryptoHelper.checkString, newKey, CryptoHelper.ENCRYPT));
            //enter entries
            for(int e=0; e<numberOfEntries; e++) {
                bw.newLine();
                bw.write(CryptoHelper.crypt(entries.get(e), newKey, CryptoHelper.ENCRYPT));
            }
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

}
