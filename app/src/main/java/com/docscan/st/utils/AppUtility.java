package com.docscan.st.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.docscan.st.R;
import com.docscan.st.db.models.Note;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by droidNinja on 19/04/16.
 */
public class AppUtility {
    /**
     * Create a File for saving an image
     */
    public static final int CAMERA_REQUEST_CODE = 0x9812;
    public static final int REQUEST_CODE_QR_SCAN = 401;
    public static final String INTENT_DATA_NOTEGROUP_ID = "intent_data_notegroup_id";


    private static final String IMG_PREFIX = "IMG_";
    private static final String IMG_POSTFIX = ".jpg";
    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";

    public static File getOutputMediaFile(String path, String name) {
        // To be safe, we should check that the SDCard is mounted
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Timber.e("External storage " + Environment.getExternalStorageState());
            return null;
        }

        File dir = new File(path);
        // Create the storage directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Timber.e("Failed to create directory");
                return null;
            }
        }

        return new File(dir.getPath() + File.separator + name);
    }

    public static void showErrorDialog(Context context, String errorMessage) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle("Oops!");
        builder.setMessage(errorMessage);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();

    }

    public static void shareDocuments(Context context, ArrayList<Uri> uris) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
//        shareIntent.setAction(Intent.ACTION_SEND);
        // shareIntent.setType("image/jpeg");
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));

        context.startActivity(Intent.createChooser(shareIntent, "Share notes.."));
    }

    public static void shareImage(Context context, Bitmap bitmap) {
       String fileUri = "";
        try {
            File mydir = new File(Environment.getExternalStorageDirectory() + "/."+context.getString(R.string.app_name)+"/bitmap");
            if (!mydir.exists()) {
                mydir.mkdirs();
            }

            fileUri = mydir.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
            FileOutputStream outputStream = new FileOutputStream(fileUri);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        Uri uri= Uri.parse(MediaStore.Images.Media.insertImage(context.getContentResolver(), BitmapFactory.decodeFile(fileUri),null,null));
        // use intent to share image
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(share, "Share Image"));
    }

    public static void shareDocument(Context context, Uri uri) {
        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setAction(Intent.ACTION_SEND);
        // shareIntent.setType("image/jpeg");
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));

        context.startActivity(Intent.createChooser(shareIntent, "Share notes.."));
    }

    public static void askAlertDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", positiveListener)
                .setNegativeButton("No", negativeListener)
                .show();
    }

    public static void rateOnPlayStore(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, " Unable to find Play Store", Toast.LENGTH_LONG).show();
        }
    }

    public static ArrayList<Uri> getUrisFromNotes(List<Note> notes) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (int index = 0; index < notes.size(); index++) {
            uris.add(notes.get(index).getImagePath());
        }

        return uris;
    }


    public static String createImageName() {
        String timeStamp = new SimpleDateFormat(TIME_FORMAT).format(new Date());
        return IMG_PREFIX + timeStamp + IMG_POSTFIX;
    }
}
