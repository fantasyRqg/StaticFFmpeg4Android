package com.zz.combine;

import com.zz.combine.mtl.MusicTimelineBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * *Created by rqg on 3/13/17 12:35 PM.
 */

public class VideoFragmentsManager {
    private ArrayList<VideoFragment> mVideoFragments = new ArrayList<>();
    private MusicTimelineBase mMusicTimeline;
    private ArrayList<TimePoint> mTimePoints;
    private int mId;
    private long mLastTimeMills = 0;
    private int mCurr = 0;
    private TimePoint[] mOtp;

    protected boolean mAbort = false;


    public VideoFragmentsManager(MusicTimelineBase musicTimeline) {
        mMusicTimeline = musicTimeline;

        TimePoint[] otp = mMusicTimeline.getOriginalTimePoints();
        mTimePoints = new ArrayList<>(otp.length);

        Arrays.sort(otp, new Comparator<TimePoint>() {
            @Override
            public int compare(TimePoint o1, TimePoint o2) {
                return (int) (o1.getEndTimeMills() - o2.getEndTimeMills());
            }
        });

        mOtp = otp;
    }


    private void addVFInternal(VideoFragment vf) {

        if (mCurr >= mOtp.length) {
//            Log.d(TAG, "addVFInternal: curr = " + mCurr + " , opt.len = " + mOtp.length);
            return;
        }

        int bigCurr = bigCurrent(mCurr, mOtp);


//        Log.i(TAG, "addVFInternal: " + vf.getVideoDurationMills() + " big = " + (bigCurr >= 0 ? mOtp[bigCurr].getEndTimeMills() - mLastTimeMills : null)
//                + " , small = " + (mOtp[mCurr].getEndTimeMills() - mLastTimeMills));

        TimePoint timePoint;
        if (bigCurr >= 0 && vf.getVideoDurationMills() >= mOtp[bigCurr].getEndTimeMills() - mLastTimeMills) {
            //如果当前视频满足大点 时间要求
            mCurr = bigCurr + 1;
//            Log.d(TAG, "addVFInternal: big");
        } else if (vf.getVideoDurationMills() >= mOtp[mCurr].getEndTimeMills() - mLastTimeMills) {
            //小点 满足
            bigCurr = mCurr;
            mCurr++;

//            Log.d(TAG, "addVFInternal: small");
        } else {
//            Log.d(TAG, "addVFInternal: none");
            //都不满足看下一个
            return;
        }

//        Log.v(TAG, "add to timpoints");

        timePoint = new TimePoint(mOtp[bigCurr].getEndTimeMills(), mOtp[bigCurr].getType());
        timePoint.videoFragment = vf;
        mLastTimeMills = timePoint.getEndTimeMills();
        mTimePoints.add(timePoint);

    }

    public boolean isOK() {
        if (mTimePoints.size() == 0) {
//            Log.e(TAG, "isOK:  do not have any timepoints");
            return false;
        }

//        Log.i(TAG, "isOK: size = " + mTimePoints.size() + " , ml = " + mMusicTimeline.getEndTimeMills() + " , tl = " + mTimePoints.get(mTimePoints.size() - 1).getEndTimeMills());

        return mTimePoints.get(mTimePoints.size() - 1).getEndTimeMills() == mMusicTimeline.getEndTimeMills();
    }


    public void addVideoFragment(VideoFragment vf) {
        mVideoFragments.add(vf);
        addVFInternal(vf);
    }


    private int bigCurrent(int curr, TimePoint[] tp) {
        for (int i = curr; i < tp.length; i++) {
            if (tp[i].getType() == TimePoint.TIMEPOINT_BIG) {
                return i;
            }
        }

        return -1;
    }


    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public MusicTimelineBase getMusicTimeline() {
        return mMusicTimeline;
    }

    public ArrayList<TimePoint> getTimePoints() {
        return mTimePoints;
    }


    public void abort() {
        mAbort = true;
    }

    public boolean isAbort() {
        return mAbort;
    }
}
