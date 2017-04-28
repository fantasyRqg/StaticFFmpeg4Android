package com.zz.combine.scale;

import android.media.MediaCodec;

/**
 * *Created by rqg on 4/28/17 3:06 PM.
 */

public class BufferInfoUtils {
    public static String getBufferInfoString(MediaCodec.BufferInfo info) {
        return " Offset = [" + info.offset + "], Size = [" + info.size + "], TimeUs = [" + info.presentationTimeUs + "], Flags = [" + info.flags + "]";
    }
}
