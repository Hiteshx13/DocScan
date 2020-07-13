/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Zillow
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.docscan.st.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Log;

import com.docscan.st.db.models.NoteGroup;
import com.docscan.st.interfaces.PhotoSavedListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;
import com.docscan.st.R;
import com.docscan.st.db.DBManager;
import com.docscan.st.main.CameraConst;
import com.docscan.st.main.Const;

public class TransformAndSaveTask extends AsyncTask<Void, Void, File> {

    private NoteGroup noteGroup;
    private String name;
    private Bitmap bitmap;
    private PhotoSavedListener callback;

    public TransformAndSaveTask(NoteGroup noteGroup, String name, Bitmap bitmap, PhotoSavedListener callback) {
        this.name = name;
        this.bitmap = bitmap;
        this.callback = callback;
        this.noteGroup = noteGroup;
    }

    @Override
    protected File doInBackground(Void... params) {

        File photo = AppUtility.getOutputMediaFile(Const.FOLDERS.CROP_IMAGE_PATH, name);
        if (photo == null) {
            Timber.e("Error creating media file, check storage permissions");
            return null;
        }

        Bitmap scannedBitmap = bitmap;//ScannerEngine.getInstance().getMagicColorBitmap(getScannedBitmap(bitmap));

        FileOutputStream fos = null;

        if(noteGroup!=null) {
            noteGroup = DBManager.getInstance().insertNote(noteGroup, name);
        }
        else
        {
            noteGroup = DBManager.getInstance().createNoteGroup(name);
        }

        try {
            fos = new FileOutputStream(photo);

            if (scannedBitmap != null && !scannedBitmap.isRecycled()) {
                scannedBitmap.compress(Bitmap.CompressFormat.JPEG, CameraConst.COMPRESS_QUALITY, fos);
            }

        } catch (FileNotFoundException e) {
            Timber.e(e, "File not found: " + e.getMessage());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Timber.e(e, e.getMessage());
            }
        }

        return photo;
    }

    @Override
    protected void onPostExecute(File file) {
        super.onPostExecute(file);
        photoSaved(file);
    }

    private void photoSaved(File photo) {
        if (photo != null) {
            if (callback != null) {
                callback.photoSaved(photo.getPath(), photo.getName());
                callback.onNoteGroupSaved(noteGroup);
            }
        }
    }

    private Bitmap getScannedBitmap(Bitmap original) {
        Map<Integer, PointF> pointFs = getEdgePoints(original);

        if (isScanPointsValid(pointFs)) {
            float x1 = pointFs.get(0).x;
            float x2 = pointFs.get(1).x;
            float x3 = pointFs.get(2).x;
            float x4 = pointFs.get(3).x;
            float y1 = pointFs.get(0).y;
            float y2 = pointFs.get(1).y;
            float y3 = pointFs.get(2).y;
            float y4 = pointFs.get(3).y;
            Log.d("", "POints(" + x1 + "," + y1 + ")(" + x2 + "," + y2 + ")(" + x3 + "," + y3 + ")(" + x4 + "," + y4 + ")");
          //  Bitmap _bitmap = ScannerEngine.getInstance().getScannedBitmap(original, x1, y1, x2, y2, x3, y3, x4, y4);
            return original;
        } else {
            Log.d("", "Invalid scan points");
           return original;
        }
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = getOrderedPoints(pointFs);
        if (!isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {

        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    private boolean isScanPointsValid(Map<Integer, PointF> pointFs) {
        return pointFs.size() == 4;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        float[] points=null;
        List<PointF> pointFs = new ArrayList<>();
//        try{
//            points = ScannerEngine.getInstance().getPoints(tempBitmap);
//        }catch (Exception e){
//           e.printStackTrace();
//        }

        if(points==null){
            pointFs.add(new PointF(0, 0));
            pointFs.add(new PointF(0, 0));
            pointFs.add(new PointF(0, 0));
            pointFs.add(new PointF(0, 0));
        }else {

            float x1 = points[0];
            float x2 = points[1];
            float x3 = points[2];
            float x4 = points[3];

            float y1 = points[4];
            float y2 = points[5];
            float y3 = points[6];
            float y4 = points[7];

            pointFs.add(new PointF(x1, y1));
            pointFs.add(new PointF(x2, y2));
            pointFs.add(new PointF(x3, y3));
            pointFs.add(new PointF(x4, y4));
        }
        return pointFs;
    }


}
