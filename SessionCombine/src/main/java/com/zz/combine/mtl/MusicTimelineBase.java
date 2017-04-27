package com.zz.combine.mtl;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.annotation.RawRes;
import android.util.Log;

import com.zz.combine.ResourceUtils;
import com.zz.combine.SessionCombiner;
import com.zz.combine.TimePoint;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * *Created by rqg on 3/13/17 12:36 PM.
 */

public abstract class MusicTimelineBase {
    private static final String TAG = "MusicTimelineBase";
    public static final long MAX_DURATION_MILLS = 10 * 1000;


    private long mAudioDurationMills;
    private MediaExtractor mExtractor;
    private MediaFormat mMediaFormat;

    private MediaCodec mDecoder;
    private AudioTrack mAudioTrack;
    private boolean isPlay = false;
    private AudioThread mAudioThread;

    public MusicTimelineBase() {
    }

    protected void prepare(String path) throws IOException {
        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(path);


        mMediaFormat = selectAudioTrack(mExtractor);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mAudioDurationMills = Integer.parseInt(durationStr);
        mmr.release();
    }


    private MediaFormat selectAudioTrack(MediaExtractor extractor) {
        int c = extractor.getTrackCount();

        for (int i = 0; i < c; i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);

            if (trackFormat.getString(MediaFormat.KEY_MIME).contains("audio/")) {
                extractor.selectTrack(i);
                return trackFormat;
            }
        }
        return null;
    }


    public long getAudioDurationMills() {
        return mAudioDurationMills;
    }

    public MediaExtractor getExtractor() {
        return mExtractor;
    }

    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }


    public void play() throws IOException {
        stop();

        isPlay = true;

        mAudioThread = new AudioThread();

        mAudioThread.start();

    }

    public MediaCodec newMediaCodec() throws IOException {
        MediaCodec mc = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
        mc.configure(mMediaFormat, null, null, 0);

        return mc;
    }

    public void stop() {
        isPlay = false;

        if (mAudioThread != null && mAudioThread.isAlive()) {
            try {
                mAudioThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "stop: ", e);
            }
            mAudioThread = null;
        }

        if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }

    }


    private class AudioThread extends Thread {
        @Override
        public void run() {
            try {
                mDecoder = newMediaCodec();
            } catch (IOException e) {
                Log.e(TAG, "run: ", e);
                MusicTimelineBase.this.stop();
                return;
            }

            mDecoder.start();

            ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            int sampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            int buffsize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            // create an audiotrack object
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffsize,
                    AudioTrack.MODE_STREAM);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioTrack.setVolume(2.0f);
            }
            audioTrack.play();

            int TIMEOUT_US = 1000;

            while (isPlay) {
                int inIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    int sampleSize = mExtractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        // We shouldn't stop the playback at this point, just pass the EOS
                        // flag to mDecoder, we will get it again from the
                        // dequeueOutputBuffer
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                    } else {
                        mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                        mExtractor.advance();
                    }

                    int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mDecoder.getOutputBuffers();
                            break;

                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MediaFormat format = mDecoder.getOutputFormat();
                            Log.d(TAG, "New format " + format);
                            audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));

                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d(TAG, "dequeueOutputBuffer timed out!");
                            break;

                        default:
                            ByteBuffer outBuffer = outputBuffers[outIndex];

                            final byte[] chunk = new byte[info.size];
                            outBuffer.get(chunk); // Read the buffer all at once
                            outBuffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                            audioTrack.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data
                            mDecoder.releaseOutputBuffer(outIndex, false);
                            break;
                    }

                    // All decoded frames have been rendered, we can stop playing now
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }
            }

            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;

            mExtractor.release();
            mExtractor = null;

            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;


        }

    }


    /**
     * last audio point end time mills
     *
     * @return
     */
    public long getEndTimeMills() {
        TimePoint[] tps = getOriginalTimePoints();
        if (tps == null || tps.length < 1)
            return -1;
        return tps[tps.length - 1].getEndTimeMills();
    }


    public abstract TimePoint[] getOriginalTimePoints();


    public void loadMusicFile(Context context) throws IOException {

        prepare(getMusicFilePath(context));
    }

    protected String getMusicFilePath(Context context) {


        int musicRawId = getMusicRawId();
        String mp = SessionCombiner.ROOT_DIR + context.getResources().getResourceEntryName(musicRawId);

        File f = new File(mp);
        if (!f.isFile() || !f.exists()) {
            ResourceUtils.rawToFile(context, musicRawId, mp);
        }


        return mp;
    }

    @RawRes
    abstract protected int getMusicRawId();

}
