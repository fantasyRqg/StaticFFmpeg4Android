package com.zz.combine.view;

import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zz.combine.databinding.FragSessionCombineBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * *Created by rqg on 3/30/17 6:38 PM.
 */

public class SessionCombineView<T> {
    private static final String TAG = "SessionCombineView";

    public static final int ITEM_COUNT = 3;


    private FragSessionCombineBinding mBinding;
    private List<SCItem<T>> mItemViews = new ArrayList<>(ITEM_COUNT);

    private SCItemAdapter<T> mItemAdapter;
    private int mLastID;

    public SessionCombineView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        mBinding = FragSessionCombineBinding.inflate(inflater, parent, false);
        mItemAdapter = new SCItemAdapter<T>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(parent.getContext(), LinearLayoutManager.HORIZONTAL, false);
        mBinding.content.setLayoutManager(layoutManager);
        mBinding.content.setHasFixedSize(true);
        mBinding.content.setAdapter(mItemAdapter);
    }

    public SessionCombineView(ViewGroup parent, SessionCombineView<T> scv) {
        this(parent);

        mItemViews.addAll(scv.mItemViews);
        mLastID = scv.mLastID;
    }

    public View getRootView() {
        return mBinding.getRoot();
    }


    public SCItem<T> getItemViewById(int id) {
        for (SCItem<T> scv : mItemViews) {
            if (scv.getId() == id)
                return scv;
        }

        return null;
    }

    public List<SCItem<T>> getItemViews() {
        return mItemViews;
    }

    public void addItem(SCItem<T> sciv) {
        Log.d(TAG, "addItem() called with: sciv = [" + sciv.getId() + "] " + mItemViews.size());
        mItemViews.add(sciv);
        mItemAdapter.addItem(sciv);
    }


    public void removeItem(SCItem<T> sciv) {
        Log.d(TAG, "removeItem() called with: sciv = [" + sciv.getId() + "] " + mItemViews.size());
        mItemViews.remove(sciv);
        mItemAdapter.removeItem(sciv);
    }

    public void removeItem(int id) {
        mItemAdapter.removeItem(getItemViewById(id));
    }

    public void showNoCombineVideo(boolean show) {
        Log.d(TAG, "showNoCombineVideo() called with: show = [" + show + "] " + mItemViews.size());

        if (show) {
            mBinding.content.setVisibility(View.GONE);
            mBinding.noSc.setVisibility(View.VISIBLE);
        } else {
            mBinding.content.setVisibility(View.VISIBLE);
            mBinding.noSc.setVisibility(View.GONE);
        }
    }


    public int nextSCItemID() {

        return ++mLastID;
    }

    public SCItem<T> newScItem(int id) {
        SCItem<T> item = new SCItem<>(id);
        addItem(item);
        Log.d(TAG, "newScItem() called with: id = [" + id + "] " + mItemViews.size());
        return item;
    }

    public SCItem<T> newScItem() {
        return newScItem(nextSCItemID());
    }


}
