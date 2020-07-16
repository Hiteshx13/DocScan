package com.docscan.st.db;

import com.docscan.st.db.models.Note;
import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.db.models.NoteGroup_Table;
import com.docscan.st.db.models.Note_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.Date;
import java.util.List;

/**
 * Created by droidNinja on 19/04/16.
 */
public class DBManager {
    private static final String TAG = DBManager.class.getSimpleName();
    private static DBManager ourInstance = new DBManager();

    public static DBManager getInstance() {
        return ourInstance;
    }

    private DBManager() {
    }

    public NoteGroup createNoteGroup(String noteImageName) {
        NoteGroup noteGroup = new NoteGroup();
        noteGroup.name = "New Group " + (getAllNoteGroups().size() + 1);
        noteGroup.type = "Document";
        noteGroup.save();

        noteGroup = insertNote(noteGroup, noteImageName);
        return noteGroup;
    }

    public NoteGroup insertNote(NoteGroup noteGroup, String noteImageName) {
        if (NoteGroup.id > 0) {
            Note note = new Note();
            note.name = noteImageName;
            note.noteId = NoteGroup.id;
            note.createdAt = new Date();
            note.associateNoteGroup(noteGroup);
            note.save();

            return getNoteGroup(NoteGroup.id);
        }
        return null;
    }

    public List<NoteGroup> getAllNoteGroups() {
        List<NoteGroup> noteGroups = new Select().from(NoteGroup.class).queryList();
        for (NoteGroup notegroup :
                noteGroups) {
            notegroup.notes = notegroup.getNotes();
            //Timber.i("size" + notegroup.notes.size(), notegroup.notes.size());
        }
        return noteGroups;
    }

    public NoteGroup getNoteGroup(int id) {
        NoteGroup noteGroup = SQLite.select()
                .from(NoteGroup.class)
                .where(NoteGroup_Table.id.eq(id))
                .querySingle();
        noteGroup.notes = noteGroup.getNotes();
        return noteGroup;
    }

    public void deleteNoteGroup(int id) {
        SQLite.delete(Note.class)
                .where(Note_Table.noteGroupId.eq(id))
                .query();
        SQLite.delete(NoteGroup.class)
                .where(NoteGroup_Table.id.eq(id))
                .query();
    }

    public void deleteNote(int id) {
        SQLite.delete(Note.class)
                .where(Note_Table.id.eq(id))
                .query();

    }

    public void updateNoteGroupName(int id, String name) {
//        SQLite.update(NoteGroup.class)
//                .set(NoteGroup_Table.name.eq(name))
//                .where(NoteGroup_Table.id.eq(id))
//                .query();
    }

    public void updateNoteGroupPDFInfo(int id, String pdfPath, int numOfFiles) {
//        SQLite.update(NoteGroup.class)
//                .set(NoteGroup_Table.pdfPath.eq(pdfPath))
//                .where(NoteGroup_Table.id.eq(id))
//                .query();
    }
}
