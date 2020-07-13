package com.docscan.st.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.docscan.st.activity.adapters.MultiSelector;
import com.docscan.st.activity.adapters.NoteAdapter;

import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import butterknife.BindView;
import butterknife.ButterKnife;
import vi.imagestopdf.CreatePDFListener;
import vi.imagestopdf.PDFEngine;
import com.docscan.st.R;

import com.docscan.st.activity.adapters.ParcelableSparseBooleanArray;
import com.docscan.st.db.DBManager;
import com.docscan.st.db.models.Note;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.fragment.ShareDialogFragment;
import com.docscan.st.fragment.ShareDialogFragment.ShareDialogListener;
import com.docscan.st.main.Const;
import com.docscan.st.manager.NotificationManager;
import com.docscan.st.manager.NotificationModel;
import com.docscan.st.manager.NotificationObserver;
import com.docscan.st.utils.AppUtility;
import com.docscan.st.utils.ItemOffsetDecoration;

public class NoteGroupActivity extends BaseActivity implements NotificationObserver, ShareDialogListener, CreatePDFListener {

    @BindView(R.id.noteGroup_rv)
    RecyclerView noteRecyclerView;


    private NoteGroup mNoteGroup;
    private MultiSelector multiSelector;

    public static final String IS_IN_ACTION_MODE = "IS_IN_ACTION_MODE";
    private ActionMode actionMode;
    private boolean isShareClicked;

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
        if(mNoteGroup!=null && mNoteGroup.notes.size()>0) {
            setUpNoteList(mNoteGroup.notes);
            setToolbar(mNoteGroup);
        }else
            finish();
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
                    onDeleteOptionClicked();
                    mode.finish();
                    return true;
                case R.id.share:
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
            if(adapter!=null)
                adapter.setNormalChoiceMode();
        }
    };

    private void onShareOptionClicked() {
        NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
        if(adapter!=null)
        {
            AppUtility.shareDocuments(this,adapter.getCheckedNotes());
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

                if(mNoteGroup.getNotes().size()==0) {
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
                if (isMultiSelectionEnabled()) {
                    multiSelector.checkView(view, position);
                    updateActionModeTitle();
                }
                else
                    openPreviewActivity(view, mNoteGroup, position);
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
        ImageView imageView = (ImageView) view.findViewById(R.id.note_iv);
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
        switch (notificationModel.notificationName)
        {
            case Const.NotificationConst.DELETE_DOCUMENT:
                onDeleteDocument(notificationModel);
                break;
        }
    }

    private void onDeleteDocument(NotificationModel notificationModel) {
        Note note = (Note) notificationModel.request;
        if(note!=null)
        {
            NoteAdapter adapter = (NoteAdapter) noteRecyclerView.getAdapter();
            if(adapter!=null)
            {
                adapter.deleteItem(note);
            }
        }
    }

    public void onCameraClicked(View view) {
        int[] startingLocation = new int[2];
        view.getLocationOnScreen(startingLocation);
        startingLocation[0] += view.getWidth() / 2;
        CameraActivity.startCameraFromLocation(startingLocation, this, mNoteGroup);
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_group_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onGeneratePDFClicked(MenuItem item) {
        ArrayList<File> files = getFilesFromNoteGroup();
        if(mNoteGroup.pdfPath!=null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName()))
        {
            PDFEngine.getInstance().openPDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
        }
        else {
            PDFEngine.getInstance().createPDF(this, files, this);
        }
    }

    public void onImportGalleryClicked(MenuItem item) {
       selectImageFromGallery(mNoteGroup);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == BaseScannerActivity.EXTRAS.REQUEST_PHOTO_EDIT ||
                requestCode == CameraActivity.CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                mNoteGroup = Parcels.unwrap(data.getParcelableExtra(NoteGroup.class.getSimpleName()));
                if (mNoteGroup != null) {
                    updateView(mNoteGroup);
                }
            }
        }
    }

    private void updateView(NoteGroup mNoteGroup) {
        NoteAdapter noteAdapter = (NoteAdapter) noteRecyclerView.getAdapter();
        if(noteAdapter!=null)
        {
            noteAdapter.setNotes(mNoteGroup.notes);
            noteAdapter.notifyDataSetChanged();
        }
    }

    private ArrayList<File> getFilesFromNoteGroup()
    {
        ArrayList<File> files = new ArrayList<>();
        for(int index=0;index<mNoteGroup.getNotes().size();index++)
        {
            File file = new File(mNoteGroup.getNotes().get(index).getImagePath().getPath());
            if(file.exists())
                files.add(file);
        }
        return files;
    }

    public void onShareButtonClicked(MenuItem item) {
        ShareDialogFragment bottomSheetDialogFragment = ShareDialogFragment.newInstance(this);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    public void sharePDF() {
        ArrayList<File> files = getFilesFromNoteGroup();
        if(mNoteGroup.pdfPath!=null && PDFEngine.getInstance().checkIfPDFExists(files, new File(mNoteGroup.pdfPath).getName()))
        {
            PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, new File(mNoteGroup.pdfPath));
        }
        else {
            isShareClicked = true;
            PDFEngine.getInstance().createPDF(this, files, this);
        }
    }

    @Override
    public void shareImage() {
        AppUtility.shareDocuments(this, AppUtility.getUrisFromNotes(mNoteGroup.getNotes()));
    }

    @Override
    public void onPDFGenerated(File pdfFile, int numOfImages) {
        if(pdfFile != null) {
            this.mNoteGroup.pdfPath = pdfFile.getPath();
            if (pdfFile.exists()) {
                if(!isShareClicked)
                    PDFEngine.getInstance().openPDF(NoteGroupActivity.this, pdfFile);
                else
                    PDFEngine.getInstance().sharePDF(NoteGroupActivity.this, pdfFile);

                DBManager.getInstance().updateNoteGroupPDFInfo(mNoteGroup.id, pdfFile.getPath(), numOfImages);
            }
            isShareClicked = false;
        }
    }
}
