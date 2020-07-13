package com.docscan.st.db.migration;

import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;

import com.docscan.st.db.PDFScannerDatabase;
import com.docscan.st.db.models.NoteGroup;

@Migration(version = 2, database = PDFScannerDatabase.class)
public class Migration2 extends AlterTableMigration<NoteGroup> {

        public Migration2() {
            super(NoteGroup.class);
        }

        @Override
        public void onPreMigrate() {
            super.onPreMigrate();
            addColumn(SQLiteType.TEXT, "pdfPath");
            addColumn(SQLiteType.INTEGER,"numOfImagesInPDF");
        }
    }