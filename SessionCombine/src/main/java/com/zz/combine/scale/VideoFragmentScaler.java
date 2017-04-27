package com.zz.combine.scale;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.zz.combine.SSNative;
import com.zz.combine.VideoFragment;

import java.io.IOException;

/**
 * *Created by rqg on 4/27/17 6:52 PM.
 */

public class VideoFragmentScaler {
    private static final String TAG = "VideoFragmentScaler";


    private String mSrcVideoPath;
    private String mDstVideoPath;

    private int mDstW;
    private int mDstH;


    public VideoFragmentScaler(String srcVideoPath, String dstVideoPath, int dstW, int dstH) {
        mSrcVideoPath = srcVideoPath;
        mDstVideoPath = dstVideoPath;
        mDstW = dstW;
        mDstH = dstH;


    }

    public boolean doScale() {

        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer;
        MediaCodec decoder;
        MediaCodec encoder;

        MediaFormat mediaFormat;

        int srcW, srcH;

        String mime;


        try {
            extractor.setDataSource(mSrcVideoPath);

            mediaFormat = VideoFragment.selectVideoTrack(extractor);
            muxer = new MediaMuxer(mSrcVideoPath + "tmp", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG, "doScale: ", e);
            return false;
        }

        if (mediaFormat == null)
            return false;

        srcW = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        srcH = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mime = mediaFormat.getString(MediaFormat.KEY_MIME);

        SSNative.VideoScaler vs = new SSNative.VideoScaler(srcW, srcH, mDstW, mDstH, mime);


    }


}
