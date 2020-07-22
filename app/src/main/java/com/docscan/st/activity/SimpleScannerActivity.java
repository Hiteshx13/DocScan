package com.docscan.st.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.docscan.st.R;
import com.docscan.st.activity.callbacks.OnDialogClickListener;
import com.docscan.st.utils.DialogsUtils;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {

        DialogsUtils.showMessageDialog(this, rawResult.getText(),true, new OnDialogClickListener() {
            @Override
            public void onButtonClicked(Boolean value) {

                if(rawResult.getText().contains("http")||rawResult.getText().contains("https")){
                    Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(rawResult.getText()));
                    //Uri uri = Uri.fromParts("package", getPackageName(), null);
                    //intent.setData(uri);
                    startActivity(intent);
                    finish();
                }

            }
        });
        // Do something with the result here
        Log.v("TAG", rawResult.getText()); // Prints scan results
        Log.v("TAG", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        // If you would like to resume scanning, call this method below:
        mScannerView.resumeCameraPreview(this);
    }
}