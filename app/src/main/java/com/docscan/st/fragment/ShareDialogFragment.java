package com.docscan.st.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import com.docscan.st.R;
import com.docscan.st.fragment.adapters.ItemAdapter;
import com.docscan.st.utils.BottomSheetModel;

public class ShareDialogFragment extends BottomSheetDialogFragment {

    private ShareDialogListener shareDialogListener;

    public interface ShareDialogListener{
        void sharePDF();
        void shareImage();
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public ShareDialogFragment()
    {

    }

    public static ShareDialogFragment newInstance(ShareDialogListener pickerDialogListener) {
        ShareDialogFragment fragment = new ShareDialogFragment();
        fragment.shareDialogListener = pickerDialogListener;
        return fragment;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.share_dialog_layout, null);
        dialog.setContentView(contentView);
 
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();
 
        if( behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        setUpView(contentView);
    }

    private void setUpView(View contentView) {
        RecyclerView recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new ItemAdapter(createItems(), new ItemAdapter.ItemListener() {
            @Override
            public void onItemClick(BottomSheetModel item) {
                if(shareDialogListener!=null) {
                    if (item.title.equals(getResources().getString(R.string.share_pdf))) {
                        shareDialogListener.sharePDF();
                    } else if (item.title.equals(getResources().getString(R.string.share_qr_code))) {
                        shareDialogListener.shareImage();
                    }
                }
                dismiss();
            }
        }));
    }

    public List<BottomSheetModel> createItems() {

        ArrayList<BottomSheetModel> items = new ArrayList<>();
        items.add(new BottomSheetModel(R.drawable.pdf_blue, getResources().getString(R.string.share_pdf)));
        items.add(new BottomSheetModel(R.drawable.enhance_gray, getResources().getString(R.string.share_qr_code)));

        return items;
    }

}