package com.metzner.enrico.epilepsy.epi_tools;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public abstract class PermissionsHelper {

    public static boolean isPermissionGranted(Context context, String _permission) {
        return ContextCompat.checkSelfPermission(context, _permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(@NonNull Activity activity, String permission_id,
                                         int permission_request_id, String _permission_need_info, int asksForPermission) {
        // Here, thisActivity is the current activity
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission_id) && asksForPermission<4) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showPermissionUsageInfo(activity, _permission_need_info);
        } else {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(activity, new String[]{permission_id}, permission_request_id);
            // permission_request_id is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    public static void showPermissionUsageInfo(Activity _activity, String _permission_usage_info) {
        Toast.makeText(_activity, _permission_usage_info, Toast.LENGTH_LONG).show();
    }
}
