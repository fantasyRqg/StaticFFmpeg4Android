package com.zz.combine;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.File;

/**
 * *Created by rqg on 3/20/17 2:50 PM.
 */

public class SessionCombiner implements Handler.Callback {
    private static final String TAG = "SessionCombiner";

    //    public static final int MESSAGE_TRY_COMBINE = 0;
    public static final int MESSAGE_STOP = 1;
    public static final int MESSAGE_INIT = 2;
    public static final int MESSAGE_NEW_COMBINE = 3;

    private boolean mStopMark = false;

    public static final String ROOT_DIR = Environment.getExternalStorageDirectory() + "/sessions/";
    public static String LOGO_VIDEO_PATH = SessionCombiner.ROOT_DIR + "logo.mp4";


    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private Handler mMainThreadHandler;

    private CombineInterface mCallback;

    private VideoFragmentsManager mVideoFragmentMgr = null;

    private Context mContext;


    public SessionCombiner(Context context, @NonNull CombineInterface callback) {
        mCallback = callback;
        mContext = context;
    }


    public void startCombiner() {
        Log.i(TAG, "startCombiner: ");

        mMainThreadHandler = new Handler(Looper.getMainLooper());

        mHandlerThread = new HandlerThread("session_combine");

        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper(), this);

        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_INIT));

    }

    public void stop() {
        Log.i(TAG, "stop() called");
        mStopMark = true;
//        if (mHandlerThread != null) {
//            mHandlerThread.quitSafely();
//        }


        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_STOP));
        }


    }

    public void postStop() {
        Log.i(TAG, "postStop() called");
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_STOP));
        }
    }

    public void postCombine(TestVFM vfm) {
        if (mHandler == null)
            throw new IllegalStateException("combine thread not started");

        Message msg = mHandler.obtainMessage(MESSAGE_NEW_COMBINE);

        msg.obj = vfm;

        mHandler.sendMessage(msg);
    }

//    public void handleNewVideo(String path) {
//        Log.i(TAG, "handleNewVideo() called with: LOGO_VIDEO_PATH = [" + path + "]");
//        if (mHandler == null)
//            throw new IllegalStateException("combine thread not started");
//
//        if (mOutputVideoCount >= mMaxOutputVideo) {
//            return;
//        }
//
//        Message msg = mHandler.obtainMessage(MESSAGE_TRY_COMBINE);
//
//        msg.obj = path;
//
//        mHandler.sendMessage(msg);
//
//    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
//            case MESSAGE_TRY_COMBINE:
//                handleTryCombine((String) msg.obj);
//                break;

            case MESSAGE_NEW_COMBINE:
                handleTryCombine((TestVFM) msg.obj);
                break;

            case MESSAGE_STOP:
                combineFinish();
                break;
            case MESSAGE_INIT:
                ensureDir(ROOT_DIR);
                ensureEndVideo();
                break;
            default:
                break;
        }
        return false;
    }

    private void combineFinish() {
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quitSafely();
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.combineFinished();
            }
        });

        mHandler = null;
    }


    @WorkerThread
    private void handleTryCombine(TestVFM vfm) {
        mVideoFragmentMgr = vfm.toVFM(mContext);

        if (mVideoFragmentMgr.isOK()) {
            combineSession(mVideoFragmentMgr);
        }
    }

    public static boolean ensureDir(String dir) {
        File df = new File(dir);

        return df.exists() || df.mkdirs();
    }

    private void combineSession(VideoFragmentsManager mgr) {
        if (mgr.isAbort() || mStopMark) {
            return;
        }

        int maxProgress = 300 + 1;
        final CombineTask combineTask = new CombineTask(mgr, mMainThreadHandler, maxProgress);

        mCallback.onStartCombineVideo(combineTask);
        combineTask.onStart();
        combineTask.addProgress(0);

        Log.d(TAG, "combineSession: start");
        ensureDir(ROOT_DIR);

        VideoFragmentsExporter exporter = new VideoFragmentsExporter();
        final String outPath = ROOT_DIR + "session_" + System.currentTimeMillis() + ".mp4";

        if (mgr.isAbort() || mStopMark) {
            return;
        }
        exporter.doExportVideo(mgr.getTimePoints(), outPath, new ProgressListener() {
            @Override
            public void onProgress(float progress) {
                combineTask.addProgress(progress);
            }
        });

        Log.d(TAG, "combineSession: export video");

        String filterPath = ROOT_DIR + "session_" + System.currentTimeMillis() + "_filter.mp4";

        combineTask.restLastProgress();


        if (mgr.isAbort() || mStopMark) {
            return;
        }
        mCallback.doFilter(outPath, filterPath, combineTask);

        Log.d(TAG, "combineSession: filter");

        removeFile(outPath);

        combineTask.restLastProgress();

        if (mgr.isAbort() || mStopMark) {
            return;
        }
        exporter.appendEndVideoAndAudio(filterPath,
                LOGO_VIDEO_PATH,
                mgr.getMusicTimeline(),
                mgr.getMusicTimeline().getEndTimeMills(),
                outPath, new ProgressListener() {
                    @Override
                    public void onProgress(float progress) {
                        combineTask.addProgress(progress);
                    }
                });
        if (mgr.isAbort()) {
            return;
        }

        Log.d(TAG, "combineSession: append logo and audio");

        Log.i(TAG, "combineSession: session video " + outPath);

        removeFile(filterPath);


        mVideoFragmentMgr = null;

        combineTask.onFinish(outPath);
    }

    private void removeFile(String outPath) {
        File file = new File(outPath);
        if (file.exists()) {
            file.delete();
        }
    }


    public String ensureEndVideo() {
        Log.i(TAG, "ensureEndVideo: ");
        File file = new File(LOGO_VIDEO_PATH);
        if (!file.exists()) {
            Log.i(TAG, "ensureEndVideo: generate logo video");
//
//            mMainThreadHandler.post(new Runnable() {
//                @Override
//                public void run() {
            mCallback.generateLogoVideo(LOGO_VIDEO_PATH);
//                }
//            });

        }

        return LOGO_VIDEO_PATH;
    }


    public interface CombineInterface {
        /**
         * this method is sync , when call return ,assume result video is already generated
         *
         * @param srcVideo src
         * @param outVideo out
         * @param task
         */
        @WorkerThread
        void doFilter(String srcVideo, String outVideo, CombineTask task);

        @WorkerThread
        void generateLogoVideo(String outPath);

//        @MainThread
//        void onGeneratedCombinedSession(String videoPath);

        @MainThread
        void combineFinished();

        @WorkerThread
        void onStartCombineVideo(CombineTask combineTask);

    }


    public static class CombineTask {
        private Handler mHandler;

        private OnTaskListener mListener;

        private float mMaxProgress;
        private float mProgress;
        private float mLastProgress;

        private VideoFragmentsManager mVFM;

        public CombineTask(VideoFragmentsManager vfm, Handler handler, float maxProgress) {
            mHandler = handler;
            mMaxProgress = maxProgress;
            mVFM = vfm;
            mLastProgress = 0;
        }

        public void setListener(OnTaskListener listener) {
            mListener = listener;
        }


        public VideoFragmentsManager getVFM() {
            return mVFM;
        }

        public void onStart() {
            if (mListener != null) {
                mListener.onStart();
            }
        }

        public void onFinish(final String videoPath) {
            if (mListener != null) {
                mListener.onFinish(videoPath);
            }
        }

        public void addProgress(float progress) {

            mProgress += progress - mLastProgress;
            mLastProgress = progress;

            final float p = mProgress / mMaxProgress * 100.0f;

            if (mListener != null)
                mListener.onProgress(p);

        }

        public void restLastProgress() {
            mLastProgress = 0;
        }


        public interface OnTaskListener {

            @MainThread
            void onStart();

            @MainThread
            void onFinish(String videoPath);

            /**
             * max 100
             *
             * @param progress progress
             */
            @MainThread
            void onProgress(float progress);
        }
    }
}
