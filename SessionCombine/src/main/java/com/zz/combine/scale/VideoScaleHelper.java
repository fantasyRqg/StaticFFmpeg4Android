package com.zz.combine.scale;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * *Created by rqg on 4/28/17 10:49 AM.
 */

public class VideoScaleHelper {
    private ThreadPoolExecutor mThreadPoolExecutor;
    private BlockingQueue<Runnable> mWorkQueue;

    public VideoScaleHelper() {
        mWorkQueue = new ArrayBlockingQueue<Runnable>(4);
        mThreadPoolExecutor = new ThreadPoolExecutor(4, 4, 5, TimeUnit.SECONDS, mWorkQueue);
    }

    public void scaleVideo(String srcVideoPath, String dstVideoPath, VideoFragmentScaler.OnScaleFinished callback) {
        VideoFragmentScaler videoFragmentScaler = new VideoFragmentScaler(mThreadPoolExecutor, srcVideoPath, dstVideoPath, callback);

        videoFragmentScaler.doScale();
    }

    public void release() {
        mThreadPoolExecutor.purge();
    }

}
