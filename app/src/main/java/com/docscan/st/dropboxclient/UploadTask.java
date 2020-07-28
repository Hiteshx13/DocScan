package com.docscan.st.dropboxclient;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadTask extends AsyncTask {

    private DbxClientV2 dbxClient;
    private File file;
    private Context context;
    OnDropBoxUploaded listener;

    public UploadTask(DbxClientV2 dbxClient, File file, Context context, OnDropBoxUploaded listener) {
        this.dbxClient = dbxClient;
        this.file = file;
        this.listener = listener;
        this.context = context;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            // Upload to Dropbox
            InputStream inputStream = new FileInputStream(file);
            dbxClient.files().uploadBuilder("/" + file.getName()) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream);


            Log.d("Upload Status", "Success");
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        listener.onFileUploaded("");
//        DropboxAPI.Entry filesInPath = mApi.metadata(path, 1, null, true, null);
//
//// Basing on provided code, only one file is uploaded
//        DropboxAPI.Entry uploadedFile = entries.contents.get(0);
//
//        String shareUrl = mApi.share(uploadedFile.path).url;
        // Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
    }
}