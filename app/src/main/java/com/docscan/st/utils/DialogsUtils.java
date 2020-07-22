package com.docscan.st.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.docscan.st.R;
import com.docscan.st.activity.callbacks.OnDialogClickListener;

public class DialogsUtils {

    public static void showMessageDialog(Context context, String strMessage, Boolean isCancelable, OnDialogClickListener clickListener) {

        Dialog mDialog = new Dialog(context);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_select_receipt);
        mDialog.setCancelable(isCancelable);
        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        AppCompatTextView tvMessage = mDialog.findViewById(R.id.tvMessage);
        AppCompatTextView tvOk = mDialog.findViewById(R.id.tvOK);

        tvMessage.setText(strMessage);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onButtonClicked(true);
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }

    public static void showImageDialog(Context context, Bitmap bitmap, Boolean isCancelable, OnDialogClickListener clickListener) {

        Dialog mDialog = new Dialog(context);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_image);
        mDialog.setCancelable(isCancelable);
        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        AppCompatImageView ivImage = mDialog.findViewById(R.id.ivImage);
        AppCompatTextView tvOk = mDialog.findViewById(R.id.tvOK);

        ivImage.setImageBitmap(bitmap);
        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onButtonClicked(true);
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }
}
