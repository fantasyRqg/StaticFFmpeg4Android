package com.zz.combine;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.Surface;

import com.zz.combine.mtl.MusicTimelineBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * *Created by rqg on 3/13/17 1:48 PM.
 */

public class VideoFragmentsPlayer {
    private static final String TAG = "VideoFragmentsPlayer";

    private List<TimePoint> mTimePoints;
    private MusicTimelineBase mMusicTimeline;

    private boolean isCombining = false;
    private boolean isPlay = false;


    private PlayThread mPlayThread;

    private Handler mPlayHandler;

    private int mCurrentVideoIndex;
    private long mLastEndtimeMills;
    private long mLastPlayUs;
    private long mStartWhen;


    private Surface mSurface;


    private OnVideoRectChange mVideoRectCallback;

    public VideoFragmentsPlayer(List<TimePoint> timePoints, MusicTimelineBase musicTimeline) {
        mTimePoints = timePoints;
        this.mMusicTimeline = musicTimeline;
    }


    public void play(Surface surface) {
        stop();

        isPlay = true;


        mSurface = surface;
        mLastEndtimeMills = 0;
        mPlayThread = new PlayThread();
        mPlayThread.start();

        mPlayHandler = new Handler(mPlayThread.getLooper());
        mPlayHandler.post(mVideoSwitch);


        try {
            mMusicTimeline.play();
        } catch (IOException e) {
            Log.e(TAG, "play: ", e);
        }
    }

    public void stop() {
        isPlay = false;


        mMusicTimeline.stop();

        if (mPlayHandler != null) {
            mPlayHandler.removeCallbacksAndMessages(null);
            mPlayHandler = null;
        }

        if (mPlayThread != null && mPlayThread.isAlive()) {
            mPlayThread.quitSafely();
            mPlayThread = null;
        }


    }

    private void setScalingMode(MediaCodec decoder) {
        decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
    }


    private Runnable mVideoSwitch = new Runnable() {
        @WorkerThread
        @Override
        public void run() {


            if (mTimePoints.size() < 1) {
                Log.d(TAG, "run: empty timepoint");
                return;
            }

            TimePoint currentTP = null;
            mLastPlayUs = 0;
            mLastEndtimeMills = 0;
            mStartWhen = 0;


            MediaFormat mMediaFormat = mTimePoints.get(1).videoFragment.getMediaFormat();
            MediaCodec decoder = null;


            try {
                decoder = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
            } catch (IOException e) {
                Log.e(TAG, "playVideo: ", e);
                return;
            }
            decoder.configure(mMediaFormat, mSurface, null, 0);
            decoder.start();

            setScalingMode(decoder);

            while (isPlay) {
                if (currentTP != null) {
                    mLastEndtimeMills = currentTP.getEndTimeMills();
                }

                if (mCurrentVideoIndex < mTimePoints.size()) {
                    currentTP = mTimePoints.get(mCurrentVideoIndex);


                    syncCSD(decoder, currentTP.videoFragment.getMediaFormat());

                    if (mVideoRectCallback != null)
                        mVideoRectCallback.onChange(currentTP.videoFragment.getVideoWidth()
                                , currentTP.videoFragment.getVideoHeight());

                    Log.d(TAG, "run: " + currentTP + ", delay = " + (currentTP.getEndTimeMills() - mLastEndtimeMills));
                    playVideo(decoder, currentTP.videoFragment.getExtractor(),
                            currentTP.getEndTimeMills() - mLastEndtimeMills,
                            (mCurrentVideoIndex + 1) >= mTimePoints.size());
                } else {
                    stop();
                    break;
                }

                mCurrentVideoIndex++;

            }


            decoder.stop();
            decoder.release();
        }
    };


    private boolean playVideo(MediaCodec decoder, MediaExtractor extractor, long duration, boolean fragmentsEnd) {
        Log.d(TAG, "playVideo() called with: decoder = [" + decoder + "], extractor = [" + extractor + "], duration = [" + duration + "]");

        duration *= 1000;

        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);


        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();


        boolean end = false;
        boolean eof = false;


        while (isPlay) {
            if (!eof) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    // fill inputBuffers[inputBufferIndex] with valid data
                    ByteBuffer inputBuffer = decoder.getInputBuffers()[inputIndex];

                    int sampleSize = extractor.readSampleData(inputBuffer, 0);


                    //end control by fragmentEnd
                    if (extractor.advance() && sampleSize > 0 && !end) {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime() + mLastPlayUs, 0);
                    } else {
                        Log.d(TAG, "playVideo: input BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        eof = true;
                    }

                    long playNow = extractor.getSampleTime();
                    if (playNow > duration) {
                        Log.i(TAG, "playVideo: end " + (playNow + mLastPlayUs) / 1000);
                        end = true;
                        mLastPlayUs += playNow;
                    }

                }
            }

            int outIndex = decoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    decoder.getOutputBuffers();
                    setScalingMode(decoder);
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    setScalingMode(decoder);
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    break;

                default:
                    decoder.releaseOutputBuffer(outIndex, true /* Surface init */);

                    if (mStartWhen <= 0) {
                        mStartWhen = System.currentTimeMillis();
                    }
                    try {
                        long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - mStartWhen);
//                        Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - mStartWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "playVideo: ", e);
                    }
                    break;
            }

            if (end && !fragmentsEnd) {
                Log.i(TAG, "playVideo: out " + fragmentsEnd + " , " + end);
                break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.i(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        return true;
    }


    private void syncCSD(MediaCodec decoder, MediaFormat mf) {
        ByteBuffer csd0 = mf.getByteBuffer("csd-0");
        int size = csd0.remaining();
        if (csd0 != null && size > 0) {
            while (true) {
                int index = decoder.dequeueInputBuffer(1000);
                if (index >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffers()[index];
                    inputBuffer.put(csd0);

                    decoder.queueInputBuffer(index, 0, size, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);

                    Log.i(TAG, "syncCSD: sync csd 0 " + size);
                    break;
                }
            }
        }


        ByteBuffer csd1 = mf.getByteBuffer("csd-1");
        size = csd1.remaining();
        if (csd1 != null && size > 0) {
            while (true) {
                int index = decoder.dequeueInputBuffer(1000);
                if (index >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffers()[index];
                    inputBuffer.put(csd0);

                    decoder.queueInputBuffer(index, 0, size, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);

                    Log.i(TAG, "syncCSD: sync csd 1 " + size);
                    break;
                }
            }
        }
    }

    public void setVideoRectCallback(OnVideoRectChange videoRectCallback) {
        mVideoRectCallback = videoRectCallback;
    }

    public interface OnVideoRectChange {
        void onChange(int vw, int vh);
    }

    private class PlayThread extends HandlerThread {

        PlayThread() {
            super("play_thread");
        }
    }

}
