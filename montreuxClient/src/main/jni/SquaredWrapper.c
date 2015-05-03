#include <jni.h>
#include "SquaredWrapper.h"

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_4;
}

JNIEXPORT jint JNICALL Java_com_example_montreuxclient_fastcv_SquaredWrapper_squared
  (JNIEnv * je, jclass jc, jint base)
{
        return (base*base);
}

JNIEXPORT jint JNICALL Java_com_example_montreuxclient_fastcv_SquaredWrapper_analyze
  (JNIEnv *env, jclass jc, jintArray bitmap)
 {
    jsize len = (*env)->GetArrayLength(env, bitmap);

    return len;
 }
