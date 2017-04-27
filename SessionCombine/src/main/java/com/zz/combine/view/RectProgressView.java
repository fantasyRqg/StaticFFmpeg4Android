package com.zz.combine.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * *Created by rqg on 3/31/17 10:55 AM.
 */

public class RectProgressView extends View {


    private RectF mRectF = new RectF();
    private float mProgress;
    private int mColor = Color.BLACK;
    private Paint mPaint;

    public RectProgressView(Context context) {
        this(context, null);
    }

    public RectProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RectProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();


        float radius = (float) Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2)) + 1;
        float dw = (radius - w) / 2.0f;
        float dh = (radius - h) / 2.0f;
        mRectF.set(-dw, -dh, w + dw, h + dh);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        float v = 360 * (1 - mProgress);
        canvas.drawArc(mRectF, -90 + 360 - v, v, true, mPaint);
    }


    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColor);
    }

    public float getProgress() {
        return mProgress;
    }


    public void setProgress(float progress) {
        if (progress > 1.0f) {
            progress = 1.0f;
        } else if (progress < 0.0f) {
            progress = 0.0f;
        }
        mProgress = progress;


        invalidate();
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(mColor);
    }
}
