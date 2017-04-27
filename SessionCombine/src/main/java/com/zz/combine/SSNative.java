package com.zz.combine;

import java.nio.ByteBuffer;

/**
 * *Created by rqg on 4/19/17 3:19 PM.
 */

public class SSNative {

    static {
        System.loadLibrary("session");
    }

    public static native int[] nativeClip(float[] rank, int rank_sample_rate,
                                          int n_clips, int min_clip_len,
                                          int max_clip_len, int margin_between_clips);


    protected static native long initScaler(int inW, int inH, int outW, int outH, String format);

    protected static native void scale(long point, ByteBuffer in, ByteBuffer out);

    protected static native void releaseScaler(long point);

    protected static native int inputBufferSize(long point);

    protected static native int outputBufferSize(long point);


    public static class VideoScaler {
        private long mP;

        public VideoScaler(int srcW, int srcH, int dstW, int dstH, String format) {
            mP = SSNative.initScaler(srcW, srcH, dstW, dstH, format);
        }

        public void releaseScaler() {
            SSNative.releaseScaler(mP);
        }


        public void scale(ByteBuffer in, ByteBuffer out) {
            SSNative.scale(mP, in, out);
        }

        public int getInputBufferSize() {
            return SSNative.inputBufferSize(mP);
        }

        public int getOutputBufferSize() {
            return SSNative.outputBufferSize(mP);
        }

        @Override
        protected void finalize() throws Throwable {
            releaseScaler();
            super.finalize();
        }
    }
}
