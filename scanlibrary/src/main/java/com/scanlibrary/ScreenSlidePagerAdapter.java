package com.scanlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Map;

class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

    Context context;
    ArrayList<String> imageList;
    ArrayList<ScanFragment> listFragments;
    ArrayList<Map<Integer, PointF>> points;

    public ScreenSlidePagerAdapter(Context context, ArrayList<String> imageList, ArrayList<ScanFragment> listFragments, FragmentManager fm) {
        super(fm);
        this.imageList = new ArrayList<String>();
        this.imageList.addAll(imageList);
        this.context = context;
        this.listFragments = listFragments;
        listFragments = new ArrayList<>();
    }

    public void onDoneClicked(int pos) {
        ArrayList<Map<Integer, PointF>> listPoints = new ArrayList<>();
        ArrayList<Bitmap> listBitmap = new ArrayList<>();
        ArrayList<String> listUri = new ArrayList<>();
        for (int i = 0; i < listFragments.size(); i++) {
            //  points.add();
            ScanFragment fragment = listFragments.get(i);
            listPoints.add(fragment.getPoints());
            listBitmap.add(fragment.getOriginalBitmap());
            listUri.add(fragment.getOriginalPath());
        }
        ScanFragment fragment = listFragments.get(0);
        fragment.performOnClick(listBitmap, listPoints,listUri);
    }

    @Override
    public Fragment getItem(int position) {
//        ScanFragment fragment;
//        if (listFragments.size() < imageList.size()) {
//            fragment = new ScanFragment(context);
//            Bundle bundle = new Bundle();
//            bundle.putString(IMAGES, imageList.get(position));
//            fragment.setArguments(bundle);
//            listFragments.add(fragment);
//        } else {
//            fragment = listFragments.get(position);
//        }


//        android.app.FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        fragmentTransaction.add(R.id.content, fragment);


        return listFragments.get(position);
    }

    @Override
    public int getCount() {
        return imageList.size();
    }
}