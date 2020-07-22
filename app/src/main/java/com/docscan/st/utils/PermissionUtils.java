package com.docscan.st.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static Boolean askForPermission(AppCompatActivity activity, String permission, Integer requestCode) {
        if (!isPermissionGranted(activity, permission) ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
            return false;
        } else {
            return true;
            //Toast.makeText(activity, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }


    public static Boolean isPermissionGranted(AppCompatActivity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static final Integer REQ_WRITE_EXST = 501;
    static final Integer REQ_CAMERA = 502;

    private boolean checkCamera(AppCompatActivity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission(AppCompatActivity activity) {

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                REQ_CAMERA);
    }
}
