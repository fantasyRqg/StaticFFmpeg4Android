package com.zz.combine;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.zz.combine.mtl.MusicTimelineBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * *Created by rqg on 3/14/17 1:48 PM.
 */

public class VideoFragmentsExporter {
    private static final String TAG = "VideoFragmentsExporter";


    public void doExportVideo(List<TimePoint> timePoints,
                              String outPath
            , ProgressListener progressListener) {
//        new Thread(new ExporterRunnable(timePoints, musicTimelineBase, outPath))
//                .start();

        new ExporterRunnable(timePoints, outPath, progressListener)
                .run();

    }


    private class ExporterRunnable implements Runnable {
        private List<TimePoint> mTimePoints;
        private String mOutPath;
        private ProgressListener mProgressListener;

        public ExporterRunnable(List<TimePoint> timePoints, String outPath, ProgressListener progressListener) {
            mTimePoints = timePoints;
            mOutPath = outPath;
            mProgressListener = progressListener;
        }

        @Override
        public void run() {
            if (mTimePoints == null || mTimePoints.size() < 1) {
                Log.e(TAG, "run: don't have any video fragments");
            }


            if (TextUtils.isEmpty(mOutPath)) {
                Log.e(TAG, "run: output LOGO_VIDEO_PATH empty");
            }


            TimePoint currentTP = null;

            long tLen = mTimePoints.get(mTimePoints.size() - 1).getEndTimeMills();

            MediaFormat mediaFormat = mTimePoints.get(0).videoFragment.getMediaFormat();

            MediaMuxer muxer;
            try {
                muxer = new MediaMuxer(mOutPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                Log.e(TAG, "run: ", e);
                return;
            }
            int videoID = muxer.addTrack(mediaFormat);


            muxer.start();

            long lastEndtimeMills = 0;
            int currentVideoIndex = 0;
            long lastExportUs = 0;


            while (true) {
                if (currentTP != null) {
                    lastEndtimeMills = currentTP.getEndTimeMills();
                }

                if (currentVideoIndex < mTimePoints.size()) {
                    currentTP = mTimePoints.get(currentVideoIndex);

                    Log.d(TAG, "run: " + currentTP + ", duration = " + (currentTP.getEndTimeMills() - lastEndtimeMills));

                    MediaExtractor extractor = currentTP.videoFragment.getExtractor();

                    int maxInputSize = getMaxInputSize(currentTP.videoFragment.getMediaFormat());
                    lastExportUs = exportPerVideoFragment(muxer, videoID, maxInputSize, extractor, currentTP.getEndTimeMills() - lastEndtimeMills, lastExportUs);
                } else {
                    break;
                }

                mProgressListener.onProgress(lastExportUs / 10.0f / tLen);
                currentVideoIndex++;
            }

            mProgressListener.onProgress(100f);

            muxer.stop();
            muxer.release();
        }


    }


    private int getMaxInputSize(MediaFormat mediaFormat) {
        int max = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//        Log.i(TAG, "getMaxInputSize:  " + max + " format = " + mediaFormat);
        return max;
    }

    public void appendEndVideoAndAudio(String videoPath, String endPath, MusicTimelineBase musicTimeline, long endTimeMills, String outPath, ProgressListener listener) {
        if (TextUtils.isEmpty(videoPath) || TextUtils.isEmpty(endPath) || TextUtils.isEmpty(outPath)) {
            throw new InvalidParameterException("file path empty error");
        }


        VideoFragment videoF;
        VideoFragment endF;
        try {
            videoF = new VideoFragment(videoPath);
            endF = new VideoFragment(endPath);

            videoF.prepare();
            endF.prepare();
        } catch (IOException e) {
            Log.e(TAG, "appendEndVideoAndAudio: ", e);
            return;
        }


        MediaExtractor videoExt = videoF.getExtractor();
        MediaExtractor endExt = endF.getExtractor();
        MediaExtractor musicExt = musicTimeline.getExtractor();

        MediaFormat mediaFormat = videoF.getMediaFormat();

        MediaMuxer muxer;

        try {
            muxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG, "run: ", e);
            return;
        }


        int videoID = muxer.addTrack(mediaFormat);
        int audioID = muxer.addTrack(musicTimeline.getMediaFormat());
        long lastTimeUs = 0;

        muxer.start();

        int videoMaxInputSize = getMaxInputSize(videoF.getMediaFormat());
        lastTimeUs = exportPerVideoFragment(muxer, videoID, videoMaxInputSize, videoExt, videoF.getVideoDurationMills(), lastTimeUs);
        Log.d(TAG, "appendEndVideoAndAudio: export video " + lastTimeUs);

        if (listener != null)
            listener.onProgress(50);

        int endMaxInputSize = getMaxInputSize(endF.getMediaFormat());
        lastTimeUs = exportPerVideoFragment(muxer, videoID, endMaxInputSize, endExt, MusicTimelineBase.MAX_DURATION_MILLS - videoF.getVideoDurationMills(), lastTimeUs);
        Log.d(TAG, "appendEndVideoAndAudio: export logo " + lastTimeUs);
        exportAudio(muxer, audioID, musicTimeline.getExtractor(), lastTimeUs / 1000);
        Log.d(TAG, "appendEndVideoAndAudio: export audio");
        muxer.stop();
        muxer.release();

        if (listener != null)
            listener.onProgress(100);
    }


    private void exportAudio(MediaMuxer muxer, int audioId, MediaExtractor audioExt, long duration) {
        audioExt.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);


        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);


        boolean end = false;


        while (true) {

            if (audioExt.getSampleTime() / 1000 > duration) {
                end = true;
            }

            info.presentationTimeUs = audioExt.getSampleTime();
            info.offset = 0;
            info.size = audioExt.readSampleData(buffer, info.offset);

            //noinspection WrongConstant
            info.flags = audioExt.getSampleFlags();

            if (validBufferInfo(info, buffer)) {
                muxer.writeSampleData(audioId, buffer, info);
            } else {
                Log.e(TAG, "exportAudio: invalid buffer and info");
            }

            if (!audioExt.advance() || end) {
                break;
            }
        }


    }


    /**
     * export video fragments
     *
     * @param muxer        muxer
     * @param videoId      video track id
     * @param videoExt     video extractor
     * @param duration     duration mills
     * @param lastExportUs last video end time Us
     */
    private long exportPerVideoFragment(MediaMuxer muxer, int videoId, int maxInputSize, MediaExtractor videoExt, long duration, long lastExportUs) {
        Log.d(TAG, "exportPerVideoFragment() called with: muxer = [" + muxer + "], maxInputSize = [" + maxInputSize + "], videoId = [" + videoId + "], videoExt = [" + videoExt + "], duration = [" + duration + "], lastExportUs = [" + lastExportUs + "]");

        videoExt.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo dump = new MediaCodec.BufferInfo();

        ByteBuffer buffer = ByteBuffer.allocate(maxInputSize);


        boolean end = false;


        while (true) {

            if (videoExt.getSampleTime() / 1000 > duration) {
                end = true;
            }


            info.offset = 0;
            int size;
            try {
                size = videoExt.readSampleData(buffer, info.offset);
            } catch (Exception e) {
                Log.e(TAG, "exportPerVideoFragment: ", e);
                return -1;
            }
            if (size > 0) {
                info.size = size;
                info.presentationTimeUs = videoExt.getSampleTime() + lastExportUs;
                //noinspection WrongConstant
                info.flags = videoExt.getSampleFlags();

                if (validBufferInfo(info, buffer)) {
                    muxer.writeSampleData(videoId, buffer, info);
                } else {
                    Log.e(TAG, "exportPerVideoFragment: invalid buffer and info");
                    printBufferInfo(dump);
                    printBufferInfoI(info);
                }
            }


            if (!videoExt.advance() || end) {
                return info.presentationTimeUs;
            }
        }


    }


    private void printBufferInfo(MediaCodec.BufferInfo info) {
        Log.d(TAG, "set() called with: Offset = [" + info.offset + "], Size = [" + info.size + "], TimeUs = [" + info.presentationTimeUs + "], Flags = [" + info.flags + "]");
    }

    private void printBufferInfoI(MediaCodec.BufferInfo info) {
        Log.i(TAG, "set() called with: Offset = [" + info.offset + "], Size = [" + info.size + "], TimeUs = [" + info.presentationTimeUs + "], Flags = [" + info.flags + "]");
    }

    private boolean validBufferInfo(MediaCodec.BufferInfo bufferInfo, ByteBuffer buffer) {
        boolean invalid = bufferInfo.size < 0 || bufferInfo.offset < 0
                || (bufferInfo.offset + bufferInfo.size) > buffer.capacity()
                || bufferInfo.presentationTimeUs < 0;

        return !invalid;
    }
}
