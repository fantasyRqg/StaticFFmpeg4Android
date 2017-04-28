//
// Created by ranqingguo on 4/19/17.
//


#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <cmath>

extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}

#include "ClipSelector.h"


#define  LOG_TAG    "Session_Native"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


jintArray nativeClip(JNIEnv *env, jclass type, jfloatArray rank_,
                     jint rank_sample_rate, jint n_clips,
                     jint min_clip_len, jint max_clip_len,
                     jint margin_between_clips) {
    jfloat *rank = env->GetFloatArrayElements(rank_, 0);
    unsigned int rankLen = (unsigned int) env->GetArrayLength(rank_);

    std::vector<double> vRank(rankLen, 0.0f);
    for (int i = 0; i < rankLen; ++i) {
        vRank[i] = rank[i];
    }

    ClipSelector selector(vRank, rank_sample_rate);

    auto result = selector.Select(n_clips, min_clip_len, max_clip_len, margin_between_clips);


    int rLen = result.size();
    int arrLen = rLen * 3;
    int clips[arrLen];

    for (int i = 0; i < rLen; ++i) {
        auto r = result[i];
        clips[i * 3] = r.first * rank_sample_rate;
        clips[i * 3 + 1] = r.second * rank_sample_rate;
        float mark = 0;
        for (int j = r.first; j <= r.second; ++j) {
            mark += rank[j];
        }
        clips[i * 3 + 2] = (int) (mark / (r.second - r.first) * 10000);
    }
    env->ReleaseFloatArrayElements(rank_, rank, 0);

    auto jResult = env->NewIntArray(arrLen);
    env->SetIntArrayRegion(jResult, 0, arrLen, clips);

    return jResult;
}


jboolean resampleVideo(JNIEnv *env, jclass type, jstring videoPath_) {
    const char *videoPath = env->GetStringUTFChars(videoPath_, 0);
    LOGI("start swscale test %s", videoPath);
    env->ReleaseStringUTFChars(videoPath_, videoPath);

    struct SwsContext *sws;
    sws = sws_getContext(1920, 1080, AV_PIX_FMT_YUV420P, 1280, 720, AV_PIX_FMT_YUV420P,
                         SWS_FAST_BILINEAR, NULL, NULL, NULL);


    return JNI_TRUE;
}


typedef struct {
    int src_w;
    int src_h;
    int dst_w;
    int dst_h;

    int src_buffer_size;
    int dst_buffer_size;
    uint8_t *src_data[4], *dst_data[4];
    int src_linesize[4], dst_linesize[4];

    AVPixelFormat src_format;
    AVPixelFormat dst_format;
    SwsContext *sws_ctx;

} ScaleData;

jlong initScaler(JNIEnv *env, jclass type, jint inW, jint inH,
                 jint outW, jint outH, jstring format_) {
    const char *format = env->GetStringUTFChars(format_, 0);

    AVPixelFormat pixelFormat = AV_PIX_FMT_NV12;

    SwsContext *sws = sws_getContext(inW, inH, pixelFormat, outW, outH, pixelFormat, SWS_POINT,
                                     NULL, NULL, NULL);

    env->ReleaseStringUTFChars(format_, format);


    ScaleData *sd = new ScaleData();

    if (!sws) {
        return 0;
    }

    sd->src_w = inW;
    sd->src_h = inH;
    sd->dst_w = outW;
    sd->dst_h = outH;
    sd->sws_ctx = sws;
    sd->src_format = pixelFormat;

    int ret;
    /* allocate source and destination image buffers */
    if ((ret = av_image_alloc(sd->src_data, sd->src_linesize,
                              sd->src_w, sd->src_h, sd->src_format, 16)) < 0) {
        fprintf(stderr, "Could not allocate source image\n");

        return 0;
    }


    sd->src_buffer_size = ret;

    /* buffer is going to be written to rawvideo file, no alignment */
    if ((ret = av_image_alloc(sd->dst_data, sd->dst_linesize,
                              sd->dst_w, sd->dst_h, sd->src_format, 1)) < 0) {
        fprintf(stderr, "Could not allocate destination image\n");
        return 0;
    }

    sd->dst_buffer_size = ret;


    return (jlong) sd;
}

jint scale(JNIEnv *env, jclass type, jlong point, jobject in,
           jobject out) {
    void *inputPtr = env->GetDirectBufferAddress(in);
    void *outputPtr = env->GetDirectBufferAddress(out);

    ScaleData *sd = (ScaleData *) point;

    av_image_fill_arrays(sd->src_data, sd->src_linesize, (const uint8_t *) inputPtr,
                         sd->src_format, sd->src_w, sd->src_h, 16);

//    fill_yuv_image(sd->src_data,sd->src_linesize,sd.srdw)

    sws_scale(sd->sws_ctx, (const uint8_t *const *) sd->src_data, sd->src_linesize, 0, sd->src_h,
              sd->dst_data, sd->dst_linesize);

    int size = av_image_copy_to_buffer((uint8_t *) outputPtr, sd->dst_buffer_size,
                                       (const uint8_t *const *) sd->dst_data, sd->dst_linesize,
                                       sd->src_format, sd->dst_w, sd->dst_h, 1);

    return size;
}

void releaseScaler(JNIEnv *env, jclass type, jlong point) {

    ScaleData *sd = (ScaleData *) point;

    av_free(&(sd->src_data[0]));
    av_free(&(sd->dst_data[0]));
    sws_freeContext(sd->sws_ctx);
    delete sd;
}


jint inputBufferSize(JNIEnv *env, jclass type, jlong point) {

    return ((ScaleData *) point)->src_buffer_size;
}

jint outputBufferSize(JNIEnv *env, jclass type, jlong point) {


    return ((ScaleData *) point)->dst_buffer_size;
}

//------------------------------------------------------------------------------------------------------------
static JNINativeMethod gGLES3JniViewMethods[] = {
        {"nativeClip",       "([FIIIII)[I",                                    (void *) nativeClip},
        {"initScaler",       "(IIIILjava/lang/String;)J",                      (void *) initScaler},
        {"scale",            "(JLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)I", (void *) scale},
        {"releaseScaler",    "(J)V",                                           (void *) releaseScaler},
        {"inputBufferSize",  "(J)I",                                           (void *) inputBufferSize},
        {"outputBufferSize", "(J)I",                                           (void *) outputBufferSize},

};

static const char *classPathName = "com/zz/combine/SSNative";


/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, classPathName,
                               gGLES3JniViewMethods,
                               sizeof(gGLES3JniViewMethods) / sizeof(gGLES3JniViewMethods[0]))) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv *env = NULL;

    LOGI("JNI_OnLoad");
    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;
    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

    bail:
    return result;
}