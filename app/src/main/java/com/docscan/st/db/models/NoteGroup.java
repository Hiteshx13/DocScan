package com.docscan.st.db.models;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.OneToMany;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.parceler.Parcel;

import java.util.List;

import com.docscan.st.db.PDFScannerDatabase;

/**
 * Created by droidNinja on 19/04/16.
 */
@ModelContainer
@Table(database = PDFScannerDatabase.class)
@Parcel(analyze={NoteGroup.class})
public class NoteGroup extends BaseModel {

    @Column
    @PrimaryKey(autoincrement = true)
    public static int id;

    @Column
    public String name; //this is image name

    @Column
    public String type;

    @Column
    public String pdfPath;

    @Column
    public int numOfImagesInPDF;

    public List<Note> notes;

    @OneToMany(methods = {OneToMany.Method.SAVE, OneToMany.Method.DELETE}, variableName = "notes")
    public List<Note> getNotes() {
        if (notes == null || notes.isEmpty()) {
            notes = SQLite.select()
                    .from(Note.class)
                    .where(Note_Table.noteId.eq(id))
                    .queryList();
        }
        return notes;
    }
}