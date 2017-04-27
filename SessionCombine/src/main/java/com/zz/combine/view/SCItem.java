package com.zz.combine.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.zz.combine.databinding.ItemScBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class SCItem<T> {
    private static final String TAG = "SCItem";


    private static final int ANIMATION_DURATION = 300;

    private static final int SHOW_CONTROL = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SHOW_CREATING = 3;
    private static final int SHOW_HIDE = 4;


    @IntDef({SHOW_CONTROL, SHOW_CREATING, SHOW_PROGRESS, SHOW_HIDE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShowState {
    }

    private ItemScBinding mScBinding;
    private ObjectAnimator mObjectAnimator;
    private static final int PROGRESS_COLOR = Color.parseColor("#80000000");
    private T mData;

    private int mId;

    @ShowState
    private int mState;

    private String mThumbnailPath;
    private String mDuration;
    private OnItemClick<T> mOnItemClick;
    private float mProgress;


    public SCItem(int id) {

        mId = id;
    }


    public void bindItemView(ItemScBinding binding) {
        mScBinding = binding;
        if (!TextUtils.isEmpty(mThumbnailPath)) {
            mScBinding.thumbnail.setImageBitmap(BitmapFactory.decodeFile(mThumbnailPath));
        }
        if (!TextUtils.isEmpty(mDuration)) {
            mScBinding.duration.setText(mDuration);
        }

        if (mState == SHOW_PROGRESS) {
            setProgress(mProgress);
        }

        setOnClickListener(mOnItemClick);
        setShowState(mState);

    }

    public void clearBind() {
        if (mObjectAnimator != null && mObjectAnimator.isRunning()) {
            mObjectAnimator.cancel();
        }
        mScBinding = null;
    }


    public void startProgress(String thumbnailPath) {
        mThumbnailPath = thumbnailPath;
        if (mScBinding == null)
            return;

        Log.d(TAG, "startProgress() called with: thumbnailPath = [" + thumbnailPath + "]");


        if (!TextUtils.isEmpty(thumbnailPath))
            mScBinding.thumbnail.setImageBitmap(BitmapFactory.decodeFile(thumbnailPath));

        mScBinding.progress.setColor(PROGRESS_COLOR);


        PropertyValuesHolder tipsX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f);
        PropertyValuesHolder tipsY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f);
        PropertyValuesHolder tipsAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.0f);

        ObjectAnimator tips = ObjectAnimator.ofPropertyValuesHolder(mScBinding.scCreating, tipsX, tipsY, tipsAlpha);


        tips.setDuration(ANIMATION_DURATION);

        tips.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScBinding.scCreating.setVisibility(View.GONE);
                mScBinding.scControl.setVisibility(View.GONE);
                mScBinding.progress.setVisibility(View.VISIBLE);
                mScBinding.thumbnail.setVisibility(View.VISIBLE);
                mScBinding.progress.setProgress(0);

                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f);
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.0f, 1f);

                ObjectAnimator progress = ObjectAnimator.ofPropertyValuesHolder(mScBinding.getRoot(), scaleX, scaleY, alpha);
                progress.setDuration(ANIMATION_DURATION);


//                    finishProgress();

                progress.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        tips.start();

    }


    private void setShowState(@ShowState int state) {
        mState = state;
        if (mScBinding == null) {
            return;
        }

        switch (state) {
            case SHOW_CONTROL:
                mScBinding.scControl.setVisibility(View.VISIBLE);
                mScBinding.scCreating.setVisibility(View.GONE);
                mScBinding.progress.setVisibility(View.GONE);
                mScBinding.thumbnail.setVisibility(View.VISIBLE);
                break;
            case SHOW_CREATING:
                mScBinding.scControl.setVisibility(View.GONE);
                mScBinding.scCreating.setVisibility(View.VISIBLE);
                mScBinding.progress.setVisibility(View.GONE);
                mScBinding.thumbnail.setVisibility(View.GONE);
                break;
            case SHOW_PROGRESS:
                mScBinding.scControl.setVisibility(View.GONE);
                mScBinding.scCreating.setVisibility(View.GONE);
                mScBinding.progress.setVisibility(View.VISIBLE);
                mScBinding.thumbnail.setVisibility(View.VISIBLE);
                break;
            case SHOW_HIDE:
                mScBinding.scControl.setVisibility(View.GONE);
                mScBinding.scCreating.setVisibility(View.GONE);
                mScBinding.progress.setVisibility(View.GONE);
                mScBinding.thumbnail.setVisibility(View.GONE);
                break;
        }
    }

    public T getData() {
        return mData;
    }

    public void setData(T data) {
        mData = data;
    }


    public void setProgress(float progress) {
        setSmoothProgress(progress / 100f, null);
    }


    private void setSmoothProgress(float progress, Animator.AnimatorListener listener) {
        mProgress = progress;
        if (mScBinding == null)
            return;

        if (mObjectAnimator != null && mObjectAnimator.isRunning()) {
            mObjectAnimator.cancel();
        }
        float cp = mScBinding.progress.getProgress();
        mObjectAnimator = ObjectAnimator
                .ofFloat(mScBinding.progress,
                        "progress",
                        cp,
                        progress);

        if (listener != null)
            mObjectAnimator.addListener(listener);

        mObjectAnimator.setDuration(Math.abs((long) (3000 * (progress - cp))));
        mObjectAnimator.start();

    }


    public void finishProgress() {
        Log.d(TAG, "finishProgress() called");

        setSmoothProgress(1.0f, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setShowState(SHOW_CONTROL);
                mScBinding.scControl.setAnimation(
                        AnimationUtils.loadAnimation(mScBinding.getRoot().getContext(),
                                android.R.anim.fade_in));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }


    public void showCreating() {
        Log.d(TAG, "showCreating() called");
        setShowState(SHOW_CREATING);
    }

    public void showPreview(String path) {
        mThumbnailPath = path;
        if (mScBinding == null) {
            return;
        }

        Log.d(TAG, "showPreview() called with: path = [" + path + "]");
        mScBinding.thumbnail.setImageBitmap(BitmapFactory.decodeFile(path));
        setShowState(SHOW_CONTROL);
    }

    public void hide() {
        setShowState(SHOW_HIDE);
    }


    public void setDuration(String duration) {
        mDuration = duration;
        if (mScBinding == null)
            return;

        mScBinding.duration.setText(duration);
    }

    public void setOnClickListener(OnItemClick<T> listener) {
        mOnItemClick = listener;
        if (mScBinding == null)
            return;

        mScBinding.scControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClick != null)
                    mOnItemClick.onClick(SCItem.this);
            }
        });
    }


    public interface OnItemClick<T> {
        void onClick(SCItem<T> itemView);
    }


    public int getId() {
        return mId;
    }
}