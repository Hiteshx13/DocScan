package com.docscan.st.utils;

import android.os.AsyncTask;

import com.docscan.st.db.models.NoteGroup;

public class DeletePhotoTask extends AsyncTask<Void, Void, Void> {

    private final NoteGroup noteGroup;

    public DeletePhotoTask(NoteGroup noteGroup) {
        this.noteGroup = noteGroup;
    }

    @Override
    protected Void doInBackground(Void... params) {
        PhotoUtil.deleteNoteGroup(noteGroup);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
