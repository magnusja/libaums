#include <errno.h>
#include <string.h>
#include <jni.h>

JNIEXPORT jint JNICALL
Java_com_github_mjdev_libaums_ErrNo_getErrnoNative(JNIEnv *env, jobject thiz) {
    return errno;
}

JNIEXPORT jstring JNICALL
        Java_com_github_mjdev_libaums_ErrNo_getErrstrNative(JNIEnv *env, jobject thiz) {
    char *error = strerror(errno);
    return (*env)->NewStringUTF(env, error);
}