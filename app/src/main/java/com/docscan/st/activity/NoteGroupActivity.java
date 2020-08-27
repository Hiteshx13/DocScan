package com.docscan.st.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.docscan.st.R;
import com.docscan.st.activity.adapters.MultiSelector;
import com.docscan.st.activity.adapters.NoteAdapter;
import com.docscan.st.activity.adapters.ParcelableSparseBooleanArray;
import com.docscan.st.activity.callbacks.OnDialogClickListener;
import com.docscan.st.db.DBManager;
import com.docscan.st.db.models.Note;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.dropboxclient.DropboxClient;
import com.docscan.st.dropboxclient.UploadFileTask;
import com.docscan.st.fragment.ShareDialogFragment;
import com.docscan.st.fragment.ShareDialogFragment.ShareDialogListener;
import com.docscan.st.googledrive.DriveServiceHelper;
import com.docscan.st.main.Const;
import com.docscan.st.manager.NotificationManager;
import com.docscan.st.manager.NotificationModel;
import com.docscan.st.manager.NotificationObserver;
import com.docscan.st.utils.AppUtility;
import com.docscan.st.utils.DialogsUtils;
import com.docscan.st.utils.ItemOffsetDecoration;
import com.docscan.st.utils.PermissionUtils;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import butterknife.BindView;
import butterknife.ButterKnife;
import vi.imagestopdf.CreatePDFListener;
import vi.imagestopdf.PDFEngine;

import static com.docscan.st.activity.CameraActivity.IMAGES;
import static com.docscan.st.utils.AppUtility.CAMERA_REQUEST_CODE;
import static com.docscan.st.utils.AppUtility.INTENT_DATA_NOTEGROUP_ID;

public class NoteGroupActivity extends BaseActivity implements NotificationObserver, ShareDialogListener, CreatePDFListener {

    @BindView(R.id.noteGroup_rv)
    RecyclerView noteRecyclerView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;


    private NoteGroup mNoteGroup;
    private MultiSelector multiSelector;

    public static final String IS_IN_ACTION_MODE = "IS_IN_ACTION_MODE";
    private ActionMode actionMode;

    static final Integer REQ_WRITE_EXST = 501;
    private boolean isShareClicked;
    private boolean isSharingQR = false;
    DriveServiceHelper mDriveHelper;
    Drive googleDriveServis;
    String accessToken;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_group);
        ButterKnife.bind(this);
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

    private void init() {
        registerNotifications();
        mNoteGroup = Parcels.unwrap(getIntent().getParcelableExtra(NoteGroup.class.getSimpleName()));

        multiSelector = new MultiSelector(noteRecyclerView);
        if (mNoteGroup != null && mNoteGroup.notes.size() > 0) {
            setUpNoteList(mNoteGroup.notes);
            setToolbar(mNoteGroup);
        } else
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(isShareClicked){
////            performGoogleLogin();
//            performDropBoxLogin();
//        }
    }

    private void updateActionModeTitle() {
        actionMode.setTitle(String.valueOf(multiSelector.getCount()));
    }

    private void startActionMode() {
//        toolbar.setVisibility(View.GONE);
        actionMode = startSupportActionMode(actionModeCallback);
    }

    private boolean isMultiSelectionEnabled() {
        return actionMode != null;
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_note, menu);
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
                    onDeleteOptionClicked();
                    mode.finish();
                    return true;
                case R.id.scan_qr:
                    onShareOptionClicked();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
//            toolbar.setVisibility(View.VISIBLE);
            actionMode = null;
//            multiSelector.clearAll();

            NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
            if (adapter != null)
                adapter.setNormalChoiceMode();
        }
    };

    private void onShareOptionClicked() {
        NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
        if (adapter != null) {
            AppUtility.shareDocuments(this, adapter.getCheckedNotes());
        }
    }

    private void onDeleteOptionClicked() {
        AppUtility.askAlertDialog(this, Const.DELETE_ALERT_TITLE, Const.DELETE_ALERT_MESSAGE, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParcelableSparseBooleanArray checkItems = multiSelector.getCheckedItems();
                NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.deleteItems(checkItems);
                }

                if (mNoteGroup.getNotes().size() == 0) {
                    DBManager.getInstance().deleteNoteGroup(mNoteGroup.id);
                    finish();
                }

                multiSelector.clearAll();
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    private void setToolbar(NoteGroup mNoteGroup) {
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(mNoteGroup.name);
    }

    private void setUpNoteList(List<Note> notes) {
        noteRecyclerView.setHasFixedSize(true);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        noteRecyclerView.setLayoutManager(gridLayoutManager);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.item_offset);
        noteRecyclerView.addItemDecoration(itemDecoration);

        NoteAdapter adapter = new NoteAdapter(notes, multiSelector);
        adapter.setCallback(new NoteAdapter.Callback() {
            @Override
            public void onItemClick(View view, int position, Note note) {
//                if (isMultiSelectionEnabled()) {
//                    multiSelector.checkView(view, position);
//                    updateActionModeTitle();
//                } else
//                    openPreviewActivity(view, mNoteGroup, position);
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
        noteRecyclerView.setAdapter(adapter);

    }

    private void openPreviewActivity(View view, NoteGroup mNoteGroup, int position) {
        int[] screenLocation = new int[2];
        ImageView imageView = view.findViewById(R.id.note_iv);
        imageView.getLocationOnScreen(screenLocation);
        PreviewActivity.startPreviewActivity(mNoteGroup, position, this, screenLocation, imageView.getWidth(), imageView.getHeight());
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public void registerNotifications() {
        NotificationManager.getInstance().registerNotification(Const.NotificationConst.DELETE_DOCUMENT, this);
    }

    @Override
    public void deRegisterNotifications() {
        NotificationManager.getInstance().deRegisterNotification(Const.NotificationConst.DELETE_DOCUMENT, this);
    }

    @Override
    public void update(Observable observable, Object data) {
        NotificationModel notificationModel = (NotificationModel) data;
        switch (notificationModel.notificationName) {
            case Const.NotificationConst.DELETE_DOCUMENT:
                onDeleteDocument(notificationModel);
                break;
        }
    }

    private void onDeleteDocument(NotificationModel notificationModel) {
        Note note = (Note) notificationModel.request;
        if (note != null) {
            NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
            if (adapter != null) {
                adapter.deleteItem(note);
            }
        }
    }

    public void onCameraClicked(View view) {
        int[] startingLocation = new int[2];
        view.getLocationOnScreen(startingLocation);
        startingLocation[0] += view.getWidth() / 2;
        CameraActivity.startCameraFromLocation(startingLocation, this, mNoteGroup.id);
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_group_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onGeneratePDFClicked(MenuItem item) {

        ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(this, mNoteGroup.notes.size());
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

       /* ArrayList<File> files = getFilesFromNoteGroup();
        if (mNoteGroup.pdfPath != null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName())) {
            PDFEngine.getInstance().openPDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
        } else {
            PDFEngine.getInstance().createPDF(this, files, this);
        }*/
    }

    public void onImportGalleryClicked(MenuItem item) {
        selectImageFromGallery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT ||
                requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {

                int noteGroupID = data.getIntExtra(INTENT_DATA_NOTEGROUP_ID, 0);
                if (noteGroupID != 0) {
                    mNoteGroup = DBManager.getInstance().getNoteGroup(noteGroupID);//Parcels.unwrap(data.getParcelableExtra(NoteGroup.class.getSimpleName()));
                }

                if (mNoteGroup != null) {
                    ArrayList<Uri> list = (ArrayList<Uri>) data.getSerializableExtra(IMAGES);
                    for (int i = 0; i < list.size(); i++) {
                        File file = new File(list.get(i).getPath());
                        addNoteToDB(file.getName());
                    }
                    updateView(mNoteGroup);
                }
            }
        } else if (requestCode == 400) {
            if (resultCode == RESULT_OK) {

                handleSignInIntent(data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void handleSignInIntent(Intent intent) {
        GoogleSignIn.getSignedInAccountFromIntent(intent)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {

                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(NoteGroupActivity.this,
                                Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        googleDriveServis = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName(getString(R.string.app_name)).build();

                        mDriveHelper = new DriveServiceHelper(googleDriveServis);
                        uploadPDFFile();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });

    }


    void uploadPDFFile() {
        //Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        String path = mNoteGroup.pdfPath;
        mDriveHelper.createFilePDF(path).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.d("Upload", "success " + s);
                String filePath = "https://docs.google.com/a/google.com/uc?id=" + s + "&export=download";
//                https://drive.google.com/file/d/1NDOVKPKckQIzPzCFm97mhJOMifnLDk3e/view?usp=sharing
                progressBar.setVisibility(View.GONE);
                mNoteGroup.drivePath = filePath;
                DBManager.getInstance().updateNoteDriveInfo(mNoteGroup.id, filePath);
                showQRcode(filePath);
                //Toast.makeText(NoteGroupActivity.this, "Success", Toast.LENGTH_SHORT).show();
//                DriveFile file = Drive.DriveApi.getFile(googleApiClient,driveId);
//                DriveResource.MetadataResult mdRslt = file.getMetadata(googleApiClient).await();
//                if (mdRslt != null && mdRslt.getStatus().isSuccess()) {
//                    String link = mdRslt.getMetadata().getWebContentLink();
//                    Log.d("LINK", link);
//                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Upload", "success");
                Toast.makeText(NoteGroupActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void showQRcode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            DialogsUtils.showImageDialog(NoteGroupActivity.this, bmp, false, new OnDialogClickListener() {
                @Override
                public void onButtonClicked(Boolean value) {

                    if (!value) {
                        AppUtility.shareImage(NoteGroupActivity.this, bmp);
                    }
                }
            });
            //((ImageView) findViewById(R.id.img_result_qr)).setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    void requestGooogleSignIn() {
        GoogleSignInOptions option = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, option);
        startActivityForResult(client.getSignInIntent(), 400);

    }

    void addNoteToDB(String name) {
        if (mNoteGroup != null) {
            mNoteGroup = DBManager.getInstance().insertNote(mNoteGroup, name);
        } else {
            mNoteGroup = DBManager.getInstance().createNoteGroup(name);
        }
    }

    private void updateView(NoteGroup mNoteGroup) {
        NoteAdapter noteAdapter = (NoteAdapter) noteRecyclerView.getAdapter();
        if (noteAdapter != null) {
            noteAdapter.setNotes(mNoteGroup.notes);
            noteAdapter.notifyDataSetChanged();
        }
    }

    private ArrayList<File> getFilesFromNoteGroup() {
        ArrayList<File> files = new ArrayList<>();
        for (int index = 0; index < mNoteGroup.getNotes().size(); index++) {
            File file = new File(mNoteGroup.getNotes().get(index).getImagePath().getPath());
            if (file.exists())
                files.add(file);
        }
        return files;
    }

    public void onShareButtonClicked(MenuItem item) {
        ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(this, mNoteGroup.notes.size());
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    public void saveImage() {
        if (PermissionUtils.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            File fileSource = new File(mNoteGroup.notes.get(0).getImagePath().getPath());
            try {
                moveFile(fileSource);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            DialogsUtils.showMessageDialog(this, this.getString(R.string.please_grant_all_required_permissions_from_application_setting), false, new OnDialogClickListener() {
                @Override
                public void onButtonClicked(Boolean value) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });
            //Toast.makeText(this, getString(R.string.please_grant_all_required_permissions_from_application_setting), Toast.LENGTH_SHORT).show();
        }
    }

    private void moveFile(File file) throws IOException {
        final File fileDest = new File(Environment.getExternalStorageDirectory()  /*"/" + getString(R.string.images) */+ "/" + getString(R.string.images));
        if (!fileDest.exists()) {
            fileDest.mkdirs();
        }

        File newFile = new File(fileDest, file.getName());
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            outputChannel = new FileOutputStream(newFile).getChannel();
            inputChannel = new FileInputStream(file).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            // file.delete();
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }

    @Override
    public void sharePDF() {
        isSharingQR = false;
        share(false);

    }

    @Override
    public void shareImage() {
        isSharingQR = true;
        isShareClicked = true;
        if (mNoteGroup.drivePath == null) {
            share(true);
        } else {
            showQRcode(mNoteGroup.drivePath);
        }
    }

    void share(Boolean isQR) {
        ArrayList<File> files = getFilesFromNoteGroup();
        if (mNoteGroup.pdfPath != null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName())) {
            if (isQR) {
                performGoogleLogin();
//                performDropBoxLogin();
            } else {
                PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
            }

        } else {
            isShareClicked = !isQR;
            PDFEngine.getInstance().createPDF(this, files, this);
        }
    }


    void performDropBoxLogin() {
        accessToken = Auth.getOAuth2Token();
        if (isDopBoxLogin()) {
            uploadFileInDropBox();
        } else {
            Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.app_key_dropbox));
        }
    }

    boolean isDopBoxLogin() {
        return accessToken != null && !accessToken.isEmpty();
    }

    void uploadFileInDropBox() {
        progressBar.setVisibility(View.VISIBLE);
        File file = new File(mNoteGroup.pdfPath);
        if (file != null) {
            //Initialize UploadTask
            new UploadFileTask(this, DropboxClient.getClient(accessToken), new UploadFileTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {
                    Log.d("", "");
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    Log.d("", "");
                    progressBar.setVisibility(View.GONE);
                }
            }).execute(mNoteGroup.pdfPath);
        }
    }

    void performGoogleLogin() {
        if (googleDriveServis == null) {
            requestGooogleSignIn();
        } else {
            uploadPDFFile();
        }
    }

    @Override
    public void onPDFGenerated(File pdfFile, int numOfImages) {
        if (pdfFile != null) {
            this.mNoteGroup.pdfPath = pdfFile.getPath();

            if (pdfFile.exists()) {
                if (!isShareClicked)
//                    performDropBoxLogin();
                    performGoogleLogin();

                else
                    PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, pdfFile);

                DBManager.getInstance().updateNoteGroupPDFInfo(mNoteGroup.id, pdfFile.getPath(), numOfImages);
            }
            isShareClicked = false;
        }
    }
}
