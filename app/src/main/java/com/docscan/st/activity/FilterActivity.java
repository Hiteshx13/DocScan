package com.docscan.st.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;


import org.parceler.Parcels;

import java.io.File;

import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.fragment.FilterFragment;
import com.docscan.st.R;
import com.docscan.st.interfaces.PhotoSavedListener;
import com.docscan.st.interfaces.ScanListener;
import com.docscan.st.main.Const;
import com.docscan.st.utils.AppUtility;
import com.docscan.st.utils.SavingBitmapTask;

public class FilterActivity extends BaseScannerActivity implements ScanListener{

    private FilterFragment previewFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(EXTRAS.FROM_CAMERA, false)) {
            setTitle(R.string.lbl_take_another);
        }
    }

    @Override
    protected void showPhoto(Bitmap bitmap) {
        if (previewFragment == null) {
            previewFragment = FilterFragment.newInstance(bitmap);
            setFragment(previewFragment, FilterFragment.class.getSimpleName());
        } else {
            previewFragment.setBitmap(bitmap);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXTRAS.REQUEST_PHOTO_EDIT) {
            if (resultCode == EXTRAS.RESULT_EDITED) {
                setResult(EXTRAS.RESULT_EDITED, setIntentData());
                loadPhoto();
            }
        }
    }

    @Override
    public void onRotateLeftClicked() {

    }

    @Override
    public void onRotateRightClicked() {

    }

    @Override
    public void onBackClicked() {
        onBackPressed();
    }

    @Override
    public void onOkButtonClicked(Bitmap bitmap) {
        File outputFile = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, System.currentTimeMillis() + ".jpg");
        if(outputFile!=null)
            new SavingBitmapTask(getNoteGroupFromIntent(), bitmap, outputFile.getAbsolutePath(), new PhotoSavedListener() {
                @Override
                public void photoSaved(String path, String name) {
                    if(previewFragment!=null)
                        previewFragment.hideProgressBar();
                }

                @Override
                public void onNoteGroupSaved(NoteGroup noteGroup) {
//                    openNoteGroupActivity(noteGroup);
                    setResult(noteGroup);
                    finish();
                }

            }).execute();
    }

    private void openNoteGroupActivity(NoteGroup noteGroup) {
        Intent intent = new Intent(this, NoteGroupActivity.class);
        intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));
        startActivity(intent);
        finish();
    }
}
