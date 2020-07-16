package com.docscan.st.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.docscan.st.R;
import com.docscan.st.camera.fragments.CameraFragment;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.interfaces.CameraParamsChangedListener;
import com.docscan.st.interfaces.KeyEventsListener;
import com.docscan.st.interfaces.OnGalllerySelectedCallback;
import com.docscan.st.interfaces.PhotoSavedListener;
import com.docscan.st.interfaces.PhotoTakenCallback;
import com.docscan.st.interfaces.RawPhotoTakenCallback;
import com.docscan.st.main.CameraConst;
import com.docscan.st.main.Const;
import com.docscan.st.manager.ImageManager;
import com.docscan.st.manager.SharedPrefManager;
import com.docscan.st.utils.PhotoUtil;
import com.docscan.st.utils.SavingPhotoTask;
import com.docscan.st.views.RevealBackgroundView;
import com.scanlibrary.OnBatchCompleteListener;
import com.scanlibrary.OnClearListener;
import com.scanlibrary.ScanActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.docscan.st.utils.AppUtility.CAMERA_REQUEST_CODE;
import static com.docscan.st.utils.AppUtility.INTENT_DATA_NOTEGROUP_ID;

public class CameraActivity extends BaseActivity implements RevealBackgroundView.OnStateChangeListener, PhotoTakenCallback, PhotoSavedListener, RawPhotoTakenCallback, CameraParamsChangedListener {

    public static final String PATH = "path";
    public static final String USE_FRONT_CAMERA = "use_front_camera";
    public static final String OPEN_PHOTO_PREVIEW = "open_photo_preview";
    public static final String LAYOUT_ID = "layout_id";
    public static final String CAPTURE_MODE = "CAPTURE_MODE";

    private static final String IMG_PREFIX = "IMG_";
    private static final String IMG_POSTFIX = ".jpg";
    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";
    static String IMAGES = "image_list";
    ArrayList<String> imageList = new ArrayList<>();


    private KeyEventsListener keyEventsListener;
    private PhotoSavedListener photoSavedListener;

    private String path;
    private boolean openPreview;

    private boolean saving;

    private int captureMode = CameraConst.CAPTURE_SINGLE_MODE;

    private CameraFragment fragment;
    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";
    private boolean isSingleClick = true;

    @BindView(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground;

    @BindView(R.id.fragment_content)
    View fragmentView;
    private int mNoteGroupID;

    public static void
    startCameraFromLocation(int[] startingLocation, Activity startingActivity, int mNoteGroup) {
        Intent intent = new Intent(startingActivity, CameraActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        intent.putExtra(PATH, Const.FOLDERS.PATH);
        intent.putExtra(CAPTURE_MODE, CameraConst.CAPTURE_SINGLE_MODE);
        intent.putExtra(USE_FRONT_CAMERA, false);
        intent.putExtra(INTENT_DATA_NOTEGROUP_ID, mNoteGroup);
        startingActivity.startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideActionBar();
        setContentView(R.layout.activity_with_fragment);
        ButterKnife.bind(this);
//        vRevealBackground=findViewById(R.id.vRevealBackground);
//
//                fragmentView=findViewById(R.id.fragment_content);

        setupRevealBackground(savedInstanceState);

        if (TextUtils.isEmpty(path = getIntent().getStringExtra(PATH))) {
            path = Environment.getExternalStorageDirectory().getPath();
        }
        openPreview = getIntent().getBooleanExtra(OPEN_PHOTO_PREVIEW, SharedPrefManager.i.isOpenPhotoPreview());
        if (openPreview != SharedPrefManager.i.isOpenPhotoPreview()) {
            SharedPrefManager.i.setOpenPhotoPreview(openPreview);
        }

        captureMode = getIntent().getIntExtra(CAPTURE_MODE, -1);
        if (captureMode == -1)
            captureMode = CameraConst.CAPTURE_SINGLE_MODE;

        boolean useFrontCamera = getIntent().getBooleanExtra(USE_FRONT_CAMERA, SharedPrefManager.i.useFrontCamera());
        if (useFrontCamera != SharedPrefManager.i.useFrontCamera()) {
            SharedPrefManager.i.setUseFrontCamera(useFrontCamera);
        }

        mNoteGroupID = getIntent().getIntExtra(INTENT_DATA_NOTEGROUP_ID, 0);

        init();
    }


    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setFillPaintColor(getResources().getColor(R.color.colorPrimaryDark));
        vRevealBackground.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);
            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return true;
                }
            });
        } else {
            vRevealBackground.setToFinishedFrame();
        }
    }


    private void init() {

        // int layoutId = getIntent().getIntExtra(LAYOUT_ID, -1);
        fragment = CameraFragment.newInstance(this, createCameraParams(), new OnBatchCompleteListener() {
            @Override
            public void onComplete() {
                openPreview(imageList);
            }
        }, new OnClearListener() {
            @Override
            public void onClear(boolean isBatch) {
                imageList.clear();
            }
        }, new OnGalllerySelectedCallback() {
            @Override
            public void onGallerySelected() {
                selectImageFromGallery(null);
            }
        });
        fragment.setParamsChangedListener(this);
        keyEventsListener = fragment;
        photoSavedListener = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_content, fragment)
                .commit();
    }

    private Bundle createCameraParams() {
        Bundle bundle = new Bundle();

        bundle.putInt(CameraFragment.RATIO, SharedPrefManager.i.getCameraRatio());
        bundle.putInt(CameraFragment.FLASH_MODE, SharedPrefManager.i.getCameraFlashMode());
        bundle.putInt(CameraFragment.HDR_MODE, SharedPrefManager.i.isHDR());
        bundle.putInt(CameraFragment.QUALITY, 1); //medium quality
        bundle.putInt(CameraFragment.FOCUS_MODE, SharedPrefManager.i.getCameraFocusMode());
        bundle.putBoolean(CameraFragment.FRONT_CAMERA, SharedPrefManager.i.useFrontCamera());
        bundle.putInt(CameraFragment.CAPTURE_MODE, captureMode);

        return bundle;
    }

    private String createName() {
        String timeStamp = new SimpleDateFormat(TIME_FORMAT).format(new Date());
        return IMG_PREFIX + timeStamp + IMG_POSTFIX;
    }

    @Override
    public void photoTaken(byte[] data, int orientation) {
        savePhoto(data, createName(), path, orientation);
    }

    @Override
    public void rawPhotoTaken(byte[] data) {
        Timber.d("rawPhotoTaken: data[%1d]", data.length);
    }

    private void savePhoto(byte[] data, String name, String path, int orientation) {
        saving = true;
        new SavingPhotoTask(data, name, path, orientation, this).execute();
    }

    @Override
    public void photoSaved(String path, String name) {
        imageList.add(path);
        saving = false;
//        Toast.makeText(this, "Photo " + name + " saved", Toast.LENGTH_SHORT).show();
        Timber.d("Photo " + name + " saved");
        if (CameraConst.DEBUG) {
            printExifOrientation(path);
        }

        //addNoteToDB(name);
        if (captureMode == CameraConst.CAPTURE_SINGLE_MODE) {
            if (fragment != null)
                fragment.hideProcessingDialog();
//            saveTransformedImage(path, name);


            //ArrayList<String> list = new ArrayList<>();
            //list.add(path);
            openPreview(imageList);

            // openPreview(list);
        } else {
//            fragment.setPreviewImage(path);

            if (fragment != null)
                fragment.setPreviewImage(path);
            //addNoteToDB(name);
//            saveTransformedImage(path, name);
        }

        if (photoSavedListener != null) {
            photoSavedListener.photoSaved(path, name);
        }
    }


    @Override
    public void onNoteGroupSaved(NoteGroup noteGroup) {

    }

    private void saveTransformedImage(final String path, final String name) {

        Target loadingTarget = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                /*new TransformAndSaveTask(mNoteGroup, name, bitmap, new PhotoSavedListener() {
                    String croppedPath = "";

                    @Override
                    public void photoSaved(String path, String name) {
//                        Toast.makeText(CameraActivity.this, "Photo " + name + " saved1", Toast.LENGTH_SHORT).show();
                        if (captureMode == CameraConst.CAPTURE_SINGLE_MODE) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(path);
                            openPreview(list);
                        }
                        croppedPath = path;
                    }

                    @Override
                    public void onNoteGroupSaved(NoteGroup noteGroup) {
                        mNoteGroup = noteGroup;
                        if (fragment != null)
                            fragment.setPreviewImage(croppedPath, noteGroup);
                    }
                }).execute();*/

            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }


            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }

        };

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ImageManager.i.loadPhoto(path, metrics.widthPixels, metrics.heightPixels, loadingTarget);
    }

    private void openPreview(ArrayList<String> list) {

        Intent intent = ScanActivity.getActivityIntent(this, list, mNoteGroupID);
        startActivityForResult(intent, BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
        //finish();

//        Intent intent = new Intent(this, ScannerActivity.class);
//        intent.putExtra(BaseScannerActivity.EXTRAS.PATH, path);
//        intent.putExtra(BaseScannerActivity.EXTRAS.NAME, name);
//        intent.putExtra(BaseScannerActivity.EXTRAS.FROM_CAMERA, true);
//        if (mNoteGroup != null)
//            intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(mNoteGroup));
//
//        startActivityForResult(intent, BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT);
//        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT) {
            switch (resultCode) {
                case BaseScannerActivity.EXTRAS.RESULT_DELETED:
                    String path = data.getStringExtra(BaseScannerActivity.EXTRAS.PATH);
                    PhotoUtil.deletePhoto(path);
                    break;
                case RESULT_CANCELED:
                    imageList.clear();
                    break;
                case RESULT_OK:
                    //mNoteGroup = null;
                    ArrayList<Uri> list = (ArrayList<Uri>) data.getSerializableExtra(IMAGES);

//                    for (int i = 0; i < list.size(); i++) {
//                        File file = new File(list.get(i).getPath());
//                        saveTransformedImage(file.getPath(), file.getName());
//                    }
//                    // DBManager.getInstance().createNoteGroup(name);
//


//                    mNoteGroup = Parcels.unwrap(data.getParcelableExtra(NoteGroup.class.getSimpleName()));
                    // if (mNoteGroup != null) {
                    Intent intent = new Intent();
                    intent.putExtra(IMAGES, list);
                    intent.putExtra(INTENT_DATA_NOTEGROUP_ID, mNoteGroupID);
                    setResult(RESULT_OK, intent);
                    finish();
                    // }
                    break;
                case SELECT_PHOTO:
                   // if(resultCode == RESULT_OK){
                        Uri selectedImage = data.getData();
//                        String[] filePathColumn = { MediaStore.Images.Media.DATA };
//
//                        Cursor cursor = getContentResolver().query(selectedImage,
//                                filePathColumn, null, null, null);
//                        cursor.moveToFirst();
//
//                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//                        String picturePath = cursor.getString(columnIndex);
//
//                        File file = new File(picturePath);

                        //cursor.close();
                       // openScannerActivity(picturePath, file.getName(), noteGroup);
                    //}

            }
        }
    }

    private void printExifOrientation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Timber.d("Orientation: " + orientation);
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                keyEventsListener.zoomIn();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                keyEventsListener.zoomOut();
                return true;
            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                return true;
            case KeyEvent.KEYCODE_CAMERA:
                keyEventsListener.takePhoto();
                return true;
        }
        return false;
    }

    @Override
    public void onQualityChanged(int id) {
        SharedPrefManager.i.setCameraQuality(id);
    }

    @Override
    public void onRatioChanged(int id) {
        SharedPrefManager.i.setCameraRatio(id);
    }

    @Override
    public void onFlashModeChanged(int id) {
        SharedPrefManager.i.setCameraFlashMode(id);
    }

    @Override
    public void onHDRChanged(int id) {
        SharedPrefManager.i.setHDRMode(id);
    }

    @Override
    public void onFocusModeChanged(int id) {
        SharedPrefManager.i.setCameraFocusMode(id);
    }

    @Override
    public void onCaptureModeChanged(int mode) {

        if (captureMode != mode) {
            captureMode = mode;
//            mNoteGroup = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (!saving) {
            super.onBackPressed();
        }
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            fragmentView.setVisibility(View.VISIBLE);

        } else {
            fragmentView.setVisibility(View.INVISIBLE);
        }
    }
}
