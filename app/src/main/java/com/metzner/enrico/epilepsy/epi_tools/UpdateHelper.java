package com.metzner.enrico.epilepsy.epi_tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
//import java.net.URLConnection;

public class UpdateHelper {

    final static private String REMOTE_VERSION_CHECK_FILE = "https://raw.githubusercontent.com/spatzewind/Epilepsy/master/app/release/output.json";
//    final static private String REMOTE_VERSION_CHECK_FILE = "https://doc-0o-28-docs.googleusercontent.com/docs/securesc"+
//            "/73uohjok913fscckgjonncib8f8fl0b5/kpe8mi6g2rnnkn7875l896m316lu13bd"+
//            "/1563451200000/09806301896761486283/09806301896761486283/";
    final static private String REMOTE_APK_RELEASE_FILE = "https://raw.githubusercontent.com/spatzewind/Epilepsy/master/app/release/app-release.apk";

    final static private String versionFileName = "versionCheck.json";

    static private long lastUpdateTime = 0;
    static private long timeBetweenUpdateChecks = 60000L;
    static private String currentVersion = "~.~.~";
    static private String newestVersion = "~.~.~";

    private static final String TAG = "UPDATE_HELPER";

    public static void setTimeBetweenUpdateChecks(long _tbuc) {
        timeBetweenUpdateChecks = _tbuc;
    }

    public static boolean shouldCheckForUpdates() {
        long currentTime = System.currentTimeMillis();
        boolean shouldCheck = currentTime - lastUpdateTime > timeBetweenUpdateChecks;
        Log.d(TAG, "Should check for updates: "+(shouldCheck ? "TRUE" : "FALSE")+"        <"+(currentTime-lastUpdateTime)+">");
        return shouldCheck;
    }

    private static int[] splitVersionName(@NonNull String versionName) {
        String[] parts = versionName.split("\\.");
        int[] version = new int[4];
        version[0] = (parts.length>0 ? Integer.parseInt(parts[0]) : 0);
        version[1] = (parts.length>1 ? Integer.parseInt(parts[1]) : 0);
        version[2] = (parts.length>2 ? Integer.parseInt(parts[2]) : 0);
        version[3] = (parts.length>3 ? Integer.parseInt(parts[3]) : 0);
        return version;
    }

    public static boolean isThereNewVersion() {
        if(currentVersion.equals("~.~.~")) return false;
        if(newestVersion.equals("~.~.~")) return false;
        int[] cv = splitVersionName(currentVersion);
        int[] nv = splitVersionName(newestVersion);
        Log.d(TAG, "CURRENT-VERSION: {"+cv[0]+"|"+cv[1]+"|"+cv[2]+"|"+cv[3]+"}");
        Log.d(TAG, "NEWEST-VERSION: {"+nv[0]+"|"+nv[1]+"|"+nv[2]+"|"+nv[3]+"}");
        if(nv[0] > cv[0]) return true;
        if(nv[0]<cv[0]) return false;
        if(nv[1] > cv[1]) return true;
        if(nv[1]<cv[1]) return false;
        if(nv[2] > cv[2]) return true;
        if(nv[2]<cv[2]) return false;
        return nv[3] > cv[3];
    }

    public static void startUpdateHelper(@NonNull Context context) {
        File internalFolder = context.getFilesDir();
        File versionFile = new File(internalFolder, versionFileName);
        if (versionFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(versionFile)))) {
                JSONObject json = new JSONObject(CryptoHelper.readAllFromBufferedReader(br));
                timeBetweenUpdateChecks = json.getLong("period");
                lastUpdateTime = json.getLong("time");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            currentVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
        }
    }

    public static void checkForUpdates(Context context) {
        lastUpdateTime = System.currentTimeMillis();
        saveVersionCheck(context);
        try  {
            URL url = new URL(REMOTE_VERSION_CHECK_FILE);
//            URLConnection connection = url.openConnection();
//            connection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new BufferedInputStream(url.openStream())));
            JSONObject obj = new JSONArray(CryptoHelper.readAllFromBufferedReader(br)).getJSONObject(0);
            JSONObject apkData = obj.getJSONObject("apkData");
            newestVersion = apkData.getString("versionName");
        } catch( MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch( JSONException je) {
            je.printStackTrace();
        }

        Log.d(TAG, "Version: current="+currentVersion+"  newest="+newestVersion);
    }

    public static void saveVersionCheck(@NonNull Context context) {
        File internalFolder = context.getFilesDir();
        File versionFile = new File(internalFolder, versionFileName);
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(versionFile))) {
            pw.println("{");
            pw.println("    \"period\":"+timeBetweenUpdateChecks+",");
            pw.println("    \"time\":"+lastUpdateTime+"");
            pw.println("}");
            pw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public static class UpdateApp extends AsyncTask<String, Integer, String> {
        boolean downloadSuccessful = false;
        File _saveFile = null;
        Context context;
        final View updateView;
        final ProgressBar bar;
        final TextView val;


        public UpdateApp(Context _context, View _v, ProgressBar _pb, TextView _tv) {
            context = _context;
            updateView = _v;
            bar = _pb;
            val = _tv;
        }

        @Override
        protected String doInBackground(String[] objects) {
            String _file_name = "Epilepsy/update.apk";
            _saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), _file_name);
            InputStream is = null;
            try (OutputStream os = new FileOutputStream(_saveFile)) {
                URL url = new URL(REMOTE_APK_RELEASE_FILE);
                URLConnection connection = url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();

                is = new BufferedInputStream(url.openStream());

                byte[] data = new byte[1024];
                long total = 0L;
                int count;
                int prevPercentage = -1;
                while( (count=is.read(data)) != -1) {
                    total += count;
                    os.write(data, 0, count);

                    final int percentage = (int) (total * 100L / fileLength);
                    publishProgress(percentage);
                    if(percentage>prevPercentage) {
                        prevPercentage = percentage;
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                bar.setProgress(percentage);
                                val.setText(percentage+"%");
                            }
                        });
                    }
                }
                os.flush();

                downloadSuccessful = true;
            } catch( IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateView.setVisibility(View.INVISIBLE);
                }
            });
            if(downloadSuccessful && _saveFile!=null) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri apkUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".provider", _saveFile);
                    //Uri apkUri = Uri.fromFile(_saveFile);
                    Intent updateIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    updateIntent.setData(apkUri);
                    updateIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(updateIntent);
                } else {
                    Uri apkUri = Uri.fromFile(_saveFile);
                    Intent updateIntent = new Intent(Intent.ACTION_VIEW);
                    updateIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    updateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(updateIntent);
                }
            }
            if(_saveFile==null) {
                Log.e(TAG, "UpdateAPK <- SavedFile is NULL");
            }
        }
    }
}
