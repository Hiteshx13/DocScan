package com.docscan.st.fragment.adapters;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.docscan.st.fragment.ImageFragment;

import java.util.List;

import com.docscan.st.db.models.Note;

public class NotesPagerAdapter extends FragmentPagerAdapter {

    private final List<Note> notes;

    public NotesPagerAdapter(FragmentManager fragmentManager, List<Note> notes) {
            super(fragmentManager);
            this.notes = notes;
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return notes.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return ImageFragment.newInstance(notes.get(position));
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Scan " + (position+1);
        }

    }