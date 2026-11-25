#include <errno.h>
#include <string.h>
#include <jni.h>

// Same stale-package-name bug as usb.c — upstream's symbol still targets the pre-rename
// `com.github.mjdev.libaums` package; the real class is `me.jahnen.libaums.core.ErrNo`. Fixed
// to match (see usb.c's comment for why this only started mattering once this module's own
// .so became the only one shipped).
JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_core_ErrNo_getErrnoNative(JNIEnv *env, jobject thiz) {
    return errno;
}

JNIEXPORT jstring JNICALL
        Java_me_jahnen_libaums_core_ErrNo_getErrstrNative(JNIEnv *env, jobject thiz) {
    char *error = strerror(errno);
    return (*env)->NewStringUTF(env, error);
}