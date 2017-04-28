package com.zz.combine.scale;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.zz.combine.SSNative;
import com.zz.combine.VideoFragment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * *Created by rqg on 4/27/17 6:52 PM.
 */
public class VideoFragmentScaler {
    private static final String TAG = "VideoFragmentScaler";
    private static final String VIDEO_MIME = "video/avc";
    private static final int VIDEO_720P_BIT_RATE = 6000000;
    private static final long WAIT_TIMEOUT = 200 * 1000;
    private String mSrcVideoPath;
    private String mDstVideoPath;

    private int mDstW;
    private int mDstH;
    private int mBitrate;

    private boolean mRun = false;


    private ThreadPoolExecutor mThreadPoolExecutor;
    private OnScaleFinished mOnScaleFinished;


    VideoFragmentScaler(ThreadPoolExecutor threadPoolExecutor, String srcVideoPath, String dstVideoPath, OnScaleFinished callback) {
        mSrcVideoPath = srcVideoPath;
        mDstVideoPath = dstVideoPath;
        mDstW = 1280;
        mDstH = 720;
        mBitrate = VIDEO_720P_BIT_RATE;

        mThreadPoolExecutor = threadPoolExecutor;
        mOnScaleFinished = callback;
    }

    boolean doScale() {

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

            if (mediaFormat == null)
                return false;

            srcW = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            srcH = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            mime = mediaFormat.getString(MediaFormat.KEY_MIME);

            muxer = new MediaMuxer(mDstVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(mediaFormat, null, null, 0);


            encoder = MediaCodec.createEncoderByType(VIDEO_MIME);
            MediaFormat outF = generateOutputMediaFormat(VIDEO_MIME, mDstW, mDstH, mBitrate, mediaFormat);

            encoder.configure(outF, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            Log.e(TAG, "doScale: ", e);
            return false;
        }

        SSNative.VideoScaler vs = new SSNative.VideoScaler(srcW, srcH, mDstW, mDstH, mime);

        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);


        scaleVideo(extractor, muxer, decoder, encoder, vs);


        return true;
    }

    private void scaleVideo(MediaExtractor extractor, MediaMuxer muxer, MediaCodec decoder, MediaCodec encoder, SSNative.VideoScaler vs) {

        int capacity = 4;
        mRun = true;

        BlockingQueue<PendingFrame> emptyQueue = new ArrayBlockingQueue<PendingFrame>(capacity);
        BlockingQueue<PendingFrame> filledQueue = new ArrayBlockingQueue<PendingFrame>(capacity);

        int outputBufferSize = vs.getOutputBufferSize();

        for (int i = 0; i < 4; i++) {
            emptyQueue.add(new PendingFrame(outputBufferSize));
        }


        SrcOutputRunnable srcOutputRunnable = new SrcOutputRunnable(emptyQueue, filledQueue, decoder, vs);

        DstOutputRunnable dstOutputRunnable = new DstOutputRunnable(encoder, muxer);

        mThreadPoolExecutor.execute(new SrcInputRunnable(extractor, decoder, srcOutputRunnable));

        mThreadPoolExecutor.execute(new DstInputRunnable(emptyQueue, filledQueue, encoder, dstOutputRunnable));

//        mThreadPoolExecutor.execute();
//        mThreadPoolExecutor.execute();

    }

//    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
//        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
//        for (int i = 0; i < capabilities.colorFormats.length; i++) {
//            int colorFormat = capabilities.colorFormats[i];
//            if (isRecognizedFormat(colorFormat)) {
//                return colorFormat;
//            }
//        }
////        fail("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
//        return 0;   // not reached
//    }


    private class SrcInputRunnable implements Runnable {
        private MediaExtractor mExtractor;
        private MediaCodec mDecoder;
        private SrcOutputRunnable mSrcOutputRunnable;

        public SrcInputRunnable(MediaExtractor extractor, MediaCodec decoder, SrcOutputRunnable srcOutputRunnable) {
            mExtractor = extractor;
            mDecoder = decoder;
            mSrcOutputRunnable = srcOutputRunnable;
        }

        @Override
        public void run() {
            boolean eos = false;
            int index;
            int size;
            int offset = 0;


            Log.i(TAG, "run: start");
            mDecoder.start();

            mThreadPoolExecutor.execute(mSrcOutputRunnable);

            while (!eos && mRun) {
                index = mDecoder.dequeueInputBuffer(WAIT_TIMEOUT);

                if (index >= 0) {

                    ByteBuffer buffer = mDecoder.getInputBuffers()[index];

                    size = mExtractor.readSampleData(buffer, offset);
//                info.set(0, size, mExtractor.getSampleTime(), mExtractor.getSampleFlags());


                    if (mExtractor.advance() && size > 0) {

                        int flags = mExtractor.getSampleFlags();
                        if ((flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            Log.i(TAG, "run: SrcInputRunnable eof");
                            eos = true;
                        }
                        mDecoder.queueInputBuffer(index, offset, size, mExtractor.getSampleTime(), flags);
                    } else {
                        Log.i(TAG, "run: SrcInputRunnable eof");
                        eos = true;
                        mDecoder.queueInputBuffer(index, offset, size, mExtractor.getSampleTime()
                                , MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }

                }
            }

            mExtractor.release();
        }
    }


    private class SrcOutputRunnable implements Runnable {
        private BlockingQueue<PendingFrame> mEmptyQueue;
        private BlockingQueue<PendingFrame> mFilledQueue;
        private MediaCodec mDecoder;
        private SSNative.VideoScaler mScaler;

        SrcOutputRunnable(BlockingQueue<PendingFrame> emptyQueue, BlockingQueue<PendingFrame> filledQueue, MediaCodec decoder, SSNative.VideoScaler scaler) {
            mEmptyQueue = emptyQueue;
            mFilledQueue = filledQueue;
            mDecoder = decoder;
            mScaler = scaler;
        }

        @Override
        public void run() {
            boolean eos = false;
            int index;
            int size;

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();


            PendingFrame pendingFrame = null;
            while (!eos && mRun) {
                index = mDecoder.dequeueOutputBuffer(info, WAIT_TIMEOUT);
                if (index >= 0) {
                    ByteBuffer buffer = mDecoder.getOutputBuffers()[index];

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        Log.i(TAG, "run: SrcOutputRunnable eof");
                        eos = true;
                    }

                    pendingFrame = getPendingItem(mEmptyQueue);

                    if (pendingFrame != null) {
                        size = mScaler.scale(buffer, pendingFrame.buffer);
                        pendingFrame.info.set(info.offset, size, info.presentationTimeUs, info.flags);
                        pendingFrame.buffer.position(0);
                        pendingFrame.buffer.limit(size);
                        putPendingItem(mFilledQueue, pendingFrame);

                    }

                    mDecoder.releaseOutputBuffer(index, false);

                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "run: " + mDecoder.getOutputFormat());
                }

            }

            mDecoder.stop();
            mDecoder.release();
            mScaler.releaseScaler();
        }
    }


    private class DstInputRunnable implements Runnable {
        private BlockingQueue<PendingFrame> mEmptyQueue;
        private BlockingQueue<PendingFrame> mFilledQueue;
        private MediaCodec mEncoder;

        private DstOutputRunnable mDstOutputRunnable;

        public DstInputRunnable(BlockingQueue<PendingFrame> emptyQueue, BlockingQueue<PendingFrame> filledQueue, MediaCodec encoder, DstOutputRunnable dstOutputRunnable) {
            mEmptyQueue = emptyQueue;
            mFilledQueue = filledQueue;
            mEncoder = encoder;

            mDstOutputRunnable = dstOutputRunnable;
        }

        @Override
        public void run() {
            boolean eos = false;
            int index;
            MediaCodec.BufferInfo info;

            mEncoder.start();


            mThreadPoolExecutor.execute(mDstOutputRunnable);

            while (!eos && mRun) {
                index = mEncoder.dequeueInputBuffer(WAIT_TIMEOUT);
                if (index >= 0) {
                    ByteBuffer buffer = mEncoder.getInputBuffers()[index];
                    PendingFrame pendingFrame = getPendingItem(mFilledQueue);
                    if (pendingFrame != null) {
                        buffer.put(pendingFrame.buffer);
                        info = pendingFrame.info;

                        mEncoder.queueInputBuffer(index, info.offset, info.size, info.presentationTimeUs, info.flags);

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            Log.i(TAG, "run: DstInputRunnable eof");
                            eos = true;
                        }


                        putPendingItem(mEmptyQueue, pendingFrame);
                    } else {
                        mEncoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
            }

            //do nothing release
        }
    }

    private class DstOutputRunnable implements Runnable {
        private MediaCodec mEncoder;
        private MediaMuxer mMuxer;

        DstOutputRunnable(MediaCodec encoder, MediaMuxer muxer) {
            mEncoder = encoder;
            mMuxer = muxer;
        }

        @Override
        public void run() {
            boolean eos = false;
            int index;

            int trackId = -1;
            long last = 0;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (!eos && mRun) {
                index = mEncoder.dequeueOutputBuffer(info, WAIT_TIMEOUT);

                switch (index) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        break;

                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.i(TAG, "run: out " + mEncoder.getOutputFormat());
                        trackId = mMuxer.addTrack(mEncoder.getOutputFormat());
                        mMuxer.start();
                        break;

                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;

                    default:
                        ByteBuffer buffer = mEncoder.getOutputBuffers()[index];

                        if (trackId != -1) {
                            if (last <= info.presentationTimeUs) {
                                last = info.presentationTimeUs;
                                mMuxer.writeSampleData(trackId, buffer, info);
                            } else {
                                Log.e(TAG, "order : last " + last + " curr = " + info.presentationTimeUs);
                            }


                        }

                        mEncoder.releaseOutputBuffer(index, false);

                        Log.v(TAG, "DstOutputRunnable: " + BufferInfoUtils.getBufferInfoString(info));
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            Log.i(TAG, "run: DstOutputRunnable eof");
                            eos = true;
                        }

                        break;
                }

            }

            mEncoder.stop();
            mEncoder.release();
            mMuxer.stop();
            mMuxer.release();


            if (mOnScaleFinished != null)
                mOnScaleFinished.onFinished();
        }
    }


    private PendingFrame getPendingItem(BlockingQueue<PendingFrame> emptyQueue) {
        PendingFrame pendingFrame = null;
        try {
            while (mRun && (pendingFrame = emptyQueue.poll(WAIT_TIMEOUT, TimeUnit.MICROSECONDS)) == null) {
                //empty
            }


        } catch (InterruptedException e) {
            Log.e(TAG, "getPendingItem: ", e);
        }

        return pendingFrame;
    }

    private boolean putPendingItem(BlockingQueue<PendingFrame> fillQueue, PendingFrame item) {
        try {
            while (mRun && !fillQueue.offer(item, WAIT_TIMEOUT, TimeUnit.MICROSECONDS)) {
                //empty
            }
            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "getPendingItem: ", e);
            return false;
        }

    }

    private MediaFormat generateOutputMediaFormat(String mime, int dstW, int dstH, int bitrate, MediaFormat inF) {
        MediaFormat outF = MediaFormat.createVideoFormat(mime, dstW, dstH);

        outF.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        outF.setInteger(MediaFormat.KEY_FRAME_RATE, inF.getInteger(MediaFormat.KEY_FRAME_RATE));
        outF.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        outF.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            outF.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//        }
//        outF.setInteger("level", 2048);

        return outF;
    }

    protected static class PendingFrame {
        final MediaCodec.BufferInfo info;
        final ByteBuffer buffer;

        public PendingFrame(int capacity) {
            this.info = new MediaCodec.BufferInfo();
            this.buffer = ByteBuffer.allocateDirect(capacity);
        }
    }


    public interface OnScaleFinished {
        void onFinished();
    }


    public void stop() {
        mRun = false;
    }

}
