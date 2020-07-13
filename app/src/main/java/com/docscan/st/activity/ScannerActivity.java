package com.docscan.st.activity;

import android.content.Intent;
import android.graphics.Bitmap;

import org.parceler.Parcels;

import java.io.File;

import com.docscan.st.R;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.fragment.CropFragment;
import com.docscan.st.interfaces.PhotoSavedListener;
import com.docscan.st.interfaces.ScanListener;
import com.docscan.st.main.Const;
import com.docscan.st.manager.ImageManager;
import com.docscan.st.utils.AppUtility;

public class ScannerActivity extends BaseScannerActivity implements ScanListener {

    private CropFragment cropFragment;

    @Override
    protected void showPhoto(Bitmap bitmap) {
        if (cropFragment == null) {
            cropFragment = CropFragment.newInstance(bitmap);
            setFragment(cropFragment, CropFragment.class.getSimpleName());
        } else {
            cropFragment.setBitmap(bitmap);
        }
    }

    private void openFilterActivity(String path, String name) {
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra(BaseScannerActivity.EXTRAS.PATH, path);
        intent.putExtra(BaseScannerActivity.EXTRAS.NAME, name);
        intent.putExtra(BaseScannerActivity.EXTRAS.FROM_CAMERA, false);
        NoteGroup noteGroup = getNoteGroupFromIntent();
        if (noteGroup != null)
            intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));

        startActivityForResult(intent, EXTRAS.FILTER_REQUEST_CODE);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    public void onRotateLeftClicked() {
        rotatePhoto(-90);
    }

    @Override
    public void onRotateRightClicked() {
        rotatePhoto(90);
    }

    @Override
    public void onBackClicked() {
        onBackPressed();
    }

    @Override
    public void onOkButtonClicked(final Bitmap croppedBitmap) {
        File outputFile = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, System.currentTimeMillis() + ".jpg");
        if (outputFile != null) {
            ImageManager.i.cropBitmap(outputFile.getAbsolutePath(), croppedBitmap, new PhotoSavedListener() {

                @Override
                public void photoSaved(String path, String name) {
                    openFilterActivity(path, name);
                }

                @Override
                public void onNoteGroupSaved(NoteGroup noteGroup) {

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}
