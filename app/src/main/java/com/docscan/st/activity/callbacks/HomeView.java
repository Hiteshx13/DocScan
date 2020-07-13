package com.docscan.st.activity.callbacks;

import java.util.List;

import com.docscan.st.db.models.NoteGroup;

/**
 * Created by droidNinja on 20/04/16.
 */
public interface HomeView extends BaseView{
    void loadNoteGroups(List<NoteGroup> noteGroups);

    void showEmptyMessage();
}
