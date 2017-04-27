package com.zz.combine;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.IOException;

public class VideoFragment {

    private static final String TAG = "VideoFragment";
    private int mVideoHeight;
    private int mVideoWidth;

    private MediaExtractor mExtractor;
    private MediaFormat mMediaFormat;
    protected long mVideoDurationMills;
    protected String mVideoPath;


    private boolean isPlay;
    MediaCodec mDecoder;


    public VideoFragment(String videoPath) throws IOException {
        mVideoPath = videoPath;


    }


    public void prepare() throws IOException {
        mExtractor = prepareMediaExtractor(mVideoPath);
        mMediaFormat = selectVideoTrack(mExtractor);

        Log.d(TAG, "VideoFragment: " + mMediaFormat);

        mVideoWidth = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mVideoHeight = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mVideoPath);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mVideoDurationMills = Integer.parseInt(durationStr);
        mmr.release();
    }

    public static MediaFormat selectVideoTrack(MediaExtractor extractor) {
        int trackCount = extractor.getTrackCount();

        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);

            if (trackFormat.getString(MediaFormat.KEY_MIME).contains("video/")) {
                extractor.selectTrack(i);
                return trackFormat;
            }
        }
        return null;
    }

    private MediaExtractor prepareMediaExtractor(String path) throws IOException {
        MediaExtractor me = new MediaExtractor();
        me.setDataSource(path);


        return me;
    }


    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }

    public long getVideoDurationMills() {
        return mVideoDurationMills;
    }

    public MediaExtractor getExtractor() {
        return mExtractor;
    }


    public int getVideoHeight() {
        return mVideoHeight;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }
}