package com.docscan.st.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.docscan.st.R;
import com.docscan.st.activity.adapters.MultiSelector;
import com.docscan.st.activity.adapters.NoteGroupAdapter;
import com.docscan.st.activity.adapters.ParcelableSparseBooleanArray;
import com.docscan.st.activity.callbacks.HomeView;
import com.docscan.st.activity.callbacks.OnDialogClickListener;
import com.docscan.st.db.DBManager;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.main.Const;
import com.docscan.st.presenters.HomePresenter;
import com.docscan.st.utils.AppUtility;
import com.docscan.st.utils.DialogsUtils;
import com.docscan.st.utils.ItemOffsetDecoration;
import com.docscan.st.utils.PermissionUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.docscan.st.activity.CameraActivity.IMAGES;
import static com.docscan.st.utils.AppUtility.CAMERA_REQUEST_CODE;

public class HomeActivity extends BaseActivity implements HomeView {


    @BindView(R.id.noteGroup_rv)
    RecyclerView noteGroupRecyclerView;

    @BindView(R.id.emptyView)
    TextView emptyView;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindView(R.id.fab)
    FloatingActionButton btnCamera;

    HomePresenter homePresenter;


    public static final String IS_IN_ACTION_MODE = "IS_IN_ACTION_MODE";
    private MultiSelector multiSelector;
    private ActionMode actionMode;
    private NoteGroup mNoteGroup;
    static final Integer REQ_WRITE_EXST = 501;
    static final Integer REQ_CAMERA = 502;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraClicked(v);
            }
        });
/*
        if (checkCameraPermission()) {

        } else {
            requestPermission();
        }*/
        if (PermissionUtils.askForPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQ_WRITE_EXST)
        ) {

        }/* else if (PermissionUtils.askForPermission(this, Manifest.permission.CAMERA, REQ_CAMERA)) {

        }*/// else {
        ////showApplicationSettingsDialog();
        //}
        init();
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
    }


    private void restoreSavedState(Bundle savedInstanceState) {
        // Restores the checked states
        multiSelector.onRestoreInstanceState(savedInstanceState);

        // Restore the action mode
        boolean isInActionMode = savedInstanceState.getBoolean(IS_IN_ACTION_MODE);
        if (isInActionMode) {
            startActionMode();
            updateActionModeTitle();
        }
    }

    private void updateActionModeTitle() {
        actionMode.setTitle(String.valueOf(multiSelector.getCount()));
    }

    private boolean checkCameraPermission() {
        // Permission is not granted
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQ_CAMERA);
    }

    private void startActionMode() {
        actionMode = startSupportActionMode(actionModeCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        multiSelector.onSaveInstanceState(outState);
        outState.putBoolean(IS_IN_ACTION_MODE, actionMode != null);
    }

    private void init() {
        homePresenter = new HomePresenter();
        homePresenter.attachView(this);

        setUpNoteGroupList();


        homePresenter.loadNoteGroups();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setUpNoteGroupList() {
        multiSelector = new MultiSelector(noteGroupRecyclerView);

        noteGroupRecyclerView.setHasFixedSize(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        noteGroupRecyclerView.setLayoutManager(gridLayoutManager);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        noteGroupRecyclerView.addItemDecoration(itemDecoration);

        NoteGroupAdapter adapter = new NoteGroupAdapter(this, multiSelector);
        adapter.setCallback(new NoteGroupAdapter.Callback() {
            @Override
            public void onItemClick(View view, int position, NoteGroup noteGroup) {
                if (isMultiSelectionEnabled()) {
                    multiSelector.checkView(view, position);
                    updateActionModeTitle();
                    if (multiSelector.getCount() > 1)
                        showEditActionMode(false);
                    else
                        showEditActionMode(true);
                } else
                    openNoteGroupActivity(noteGroup);
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!isMultiSelectionEnabled()) {
                    startActionMode();
                }
                multiSelector.checkView(view, position);
                updateActionModeTitle();
            }
        });
        noteGroupRecyclerView.setAdapter(adapter);
    }

    private void showEditActionMode(boolean b) {
        if (actionMode != null) {
            MenuItem menuItem = actionMode.getMenu().findItem(R.id.edit);
            if (menuItem != null)
                menuItem.setVisible(b);
        }
    }


    private boolean isMultiSelectionEnabled() {
        return actionMode != null;
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    if (multiSelector.getCount() == 0) {
                        Toast.makeText(HomeActivity.this, "Please select any document", Toast.LENGTH_SHORT).show();
                    } else {
                        onDeleteOptionClicked();
                        mode.finish();
                    }
                    return true;
               /* case R.id.share:
                    onShareOptionClicked();
                    mode.finish();
                    return true;*/
                case R.id.edit:
                    onEditOptionClicked();
                    mode.finish();
                    return true;

                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
//            multiSelector.clearAll();

            NoteGroupAdapter adapter = (NoteGroupAdapter) noteGroupRecyclerView.getAdapter();
            if (adapter != null)
                adapter.setNormalChoiceMode();
        }
    };

    private void onEditOptionClicked() {
        NoteGroupAdapter adapter = (NoteGroupAdapter) noteGroupRecyclerView.getAdapter();
        if (adapter != null) {
            NoteGroup noteGroup = adapter.getCheckedNoteGroup();
            if (noteGroup != null) {
                homePresenter.showRenameDialog(noteGroup);
            }
        }
    }

    private void onShareOptionClicked() {
        NoteGroupAdapter adapter = (NoteGroupAdapter) noteGroupRecyclerView.getAdapter();
        if (adapter != null) {
            AppUtility.shareDocuments(this, adapter.getCheckedNoteGroups());
        }
    }

    private void onDeleteOptionClicked() {
        final ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItems();
        AppUtility.askAlertDialog(this, Const.DELETE_ALERT_TITLE, Const.DELETE_ALERT_MESSAGE, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        NoteGroupAdapter adapter = (NoteGroupAdapter) noteGroupRecyclerView.getAdapter();
                        if (adapter != null) {
                            adapter.deleteItems(checkItems);
                            homePresenter.loadNoteGroups();
                        }
                        multiSelector.clearAll();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    }

    private void openNoteGroupActivity(NoteGroup noteGroup) {
        Intent intent = new Intent(this, NoteGroupActivity.class);
        intent.putExtra(NoteGroup.class.getSimpleName(), Parcels.wrap(noteGroup));
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    protected void onDestroy() {
        homePresenter.detachView();
        super.onDestroy();
    }

    public void onCameraClicked(View view) {

        if (PermissionUtils.askForPermission(this, Manifest.permission.CAMERA, REQ_CAMERA)) {
            mNoteGroup = null;
            int[] startingLocation = new int[2];
            view.getLocationOnScreen(startingLocation);
            startingLocation[0] += view.getWidth() / 2;
            CameraActivity.startCameraFromLocation(startingLocation, this, 0);
            overridePendingTransition(0, 0);
        }
//        else{
//            showApplicationSettingsDialog();
//        }


    }

    @Override
    public void loadNoteGroups(List<NoteGroup> noteGroups) {
        NoteGroupAdapter adapter = (NoteGroupAdapter) noteGroupRecyclerView.getAdapter();
        adapter.setNoteGroups(noteGroups);
        adapter.notifyDataSetChanged();
        noteGroupRecyclerView.requestFocus();

        noteGroupRecyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void showEmptyMessage() {
        noteGroupRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (homePresenter != null)
            homePresenter.loadNoteGroups();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

            if (requestCode == REQ_WRITE_EXST) {

            } else if (requestCode == REQ_CAMERA) {
                btnCamera.performClick();
            }

        } else {
            showApplicationSettingsDialog();
            // Toast.makeText(this, "Grant all required permission from application settings", Toast.LENGTH_SHORT).show();
        }
    }


    void showApplicationSettingsDialog() {
        DialogsUtils.showMessageDialog(this, this.getString(R.string.please_grant_all_required_permissions_from_application_setting), false, new OnDialogClickListener() {
            @Override
            public void onButtonClicked(Boolean value) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    public void onImportGalleryClicked(MenuItem item) {
        selectImageFromGallery();
    }

    public void onRateUsClicked(MenuItem item) {
        AppUtility.rateOnPlayStore(this);
    }

    //147041383461-oue2nri37orkgrvb4mgo29q3f91mla2i.apps.googleusercontent.com
    public void onScanQRClicked(MenuItem item) {
        //new IntentIntegrator(this).initiateScan();

        Intent intent = new Intent(this, SimpleScannerActivity.class);
        startActivity(intent);
//        if (mDriveHelper == null) {
//            requestGooogleSignIn();
//        } else {
//            uploadPDFFile();
//        }


//        AppUtility.rateOnPlayStore(this);
//        Intent i = new Intent(HomeActivity.this, QrCodeActivity.class);
//        startActivityForResult( i,REQUEST_CODE_QR_SCAN);
        //Toast.makeText(this, "QR", Toast.LENGTH_SHORT).show();
    }

    void addNoteToDB(String name) {
        if (mNoteGroup != null) {
            mNoteGroup = DBManager.getInstance().insertNote(mNoteGroup, name);
        } else {
            mNoteGroup = DBManager.getInstance().createNoteGroup(name);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<Uri> list = (ArrayList<Uri>) data.getSerializableExtra(IMAGES);
            for (int i = 0; i < list.size(); i++) {
                File file = new File(list.get(i).getPath());
                addNoteToDB(file.getName());
            }
            init();
        }
//      /*  IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result != null) {
//            if (result.getContents() == null) {
//                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
//            } else {
//                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
//            }
//        }*/


    }


    @Override
    public void onBackPressed() {
//        AppUtility.askAlertDialog(this, "Don't forget to rate us.", "Press YES to rate or NO to exit", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        AppUtility.rateOnPlayStore(HomeActivity.this);
//                    }
//                },
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });
        finishAffinity();
    }


    /*private void startScan() {
     */

    /**
     * Build a new MaterialBarcodeScanner
     *//*
        final MaterialBarcodeScanner materialBarcodeScanner = new MaterialBarcodeScannerBuilder()
                .withActivity(HomeActivity.this)
                .withEnableAutoFocus(true)
                .withBleepEnabled(true)
                .withBackfacingCamera()
                .withText("Scanning...")
                .withResultListener(new MaterialBarcodeScanner.OnResultListener() {
                    @Override
                    public void onResult(Barcode barcode) {
                        barcodeResult = barcode;
                        result.setText(barcode.rawValue);
                    }
                })
                .build();
        materialBarcodeScanner.startScan();
    }*/


}
