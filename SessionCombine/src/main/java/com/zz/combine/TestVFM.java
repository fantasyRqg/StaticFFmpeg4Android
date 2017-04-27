package com.zz.combine;

import android.content.Context;
import android.util.Log;

import com.zz.combine.mtl.MusicTimelineBase;

import java.io.IOException;
import java.util.ArrayList;

/**
 * *Created by rqg on 3/31/17 3:34 PM.
 */

public class TestVFM extends VideoFragmentsManager {
    private static final String TAG = "TestVFM";

    public TestVFM(MusicTimelineBase musicTimeline) {
        super(musicTimeline);
    }


    public VideoFragmentsManager toVFM(Context context) {
        MusicTimelineBase musicTimeline = getMusicTimeline();

        try {
            musicTimeline.loadMusicFile(context);
        } catch (IOException e) {
            Log.e(TAG, "toVFM: ", e);
            return null;
        }

//        VideoFragmentsManager vfm = new VideoFragmentsManager(musicTimeline);

        ArrayList<TimePoint> timePoints = getTimePoints();

        for (int i = 0; i < timePoints.size(); i++) {
            TestVideoFragment tvf = (TestVideoFragment) timePoints.get(i).videoFragment;
            try {
//                vfm.addVideoFragment(tvf.toVideoFragment());
                tvf.toVideoFragment();
            } catch (IOException e) {
                Log.e(TAG, "toVFM: ", e);
                return null;
            }
        }

        return this;
    }
}
