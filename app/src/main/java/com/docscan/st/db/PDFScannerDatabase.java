package com.docscan.st.db;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by droidNinja on 19/04/16.
 */
@Database(name = PDFScannerDatabase.NAME, version = PDFScannerDatabase.VERSION)
public class PDFScannerDatabase {

    public static final String NAME = "DocScanDatabase";

    public static final int VERSION = 1;
}
