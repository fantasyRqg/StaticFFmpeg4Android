package com.zz.combine.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.zz.combine.databinding.ItemScBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * *Created by rqg on 4/20/17 6:40 PM.
 */

public class SCItemAdapter<T> extends RecyclerView.Adapter<SCItemAdapter.ViewHolder<T>> {
    private List<SCItem<T>> mItemViews = new ArrayList<>(3);


    @Override
    public ViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemScBinding binding = ItemScBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder<T>(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder<T> holder, int position) {
        holder.setTSCItem(mItemViews.get(position));
    }


    @Override
    public long getItemId(int position) {
        return mItemViews.get(position).getId();
    }

    @Override
    public int getItemCount() {
        if (mItemViews == null)
            return 0;

        else
            return mItemViews.size();
    }


    public void addItem(SCItem<T> sciv) {
        mItemViews.add(sciv);
        notifyItemInserted(mItemViews.size() - 1);
    }


    public void removeItem(SCItem<T> sciv) {
        int i = mItemViews.indexOf(sciv);

        mItemViews.remove(i);
        notifyItemRemoved(i);
    }

    static class ViewHolder<T> extends RecyclerView.ViewHolder {
        private SCItem<T> mTSCItem;
        private ItemScBinding mBinding;

        public ViewHolder(ItemScBinding itemScBinding) {
            super(itemScBinding.getRoot());
            mBinding = itemScBinding;
        }

        public void setTSCItem(SCItem<T> TSCItem) {
            if (mTSCItem != null)
                mTSCItem.clearBind();

            mTSCItem = TSCItem;
            mTSCItem.bindItemView(mBinding);
        }
    }
}
