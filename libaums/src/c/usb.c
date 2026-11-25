#include <jni.h>
#include <sys/ioctl.h>
#include <linux/usbdevice_fs.h>
#include <errno.h>
#include <string.h>
#include "../../../libusbcommunication/src/c/log.h"

#define TAG "native_usb_ioctl"

// Upstream libaums (as of core:0.10.0) ships these symbol names, but the real class moved to
// me.jahnen.libaums.core.usb.AndroidUsbCommunication (note the missing "core" segment below) —
// confirmed against the real class file in this same vendored tree. The published Maven AAR
// apparently got away with this because something else (an older prebuilt .so with the correct
// old package, or JNI's lenient-binding JNI_OnLoad registration elsewhere) papered over it; once
// this module's own freshly-compiled .so is the only one that ships, a mismatched symbol means
// resetDevice()/clearFeatureHalt() throw UnsatisfiedLinkError the first time either is called —
// exactly the USB stall/error-recovery path this project cares about. Fixed to match the real
// package.
JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_core_usb_AndroidUsbCommunication_resetUsbDeviceNative(JNIEnv *env, jobject thiz, jint fd) {
    int ret = ioctl(fd, USBDEVFS_RESET);
    if(ret < 0) {
        LOG_E(TAG, "ioctl USBDEVFS_RESET error %d, %s", errno, strerror(errno));
    }

    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_core_usb_AndroidUsbCommunication_clearHaltNative(JNIEnv *env, jobject thiz, jint fd, jint endpoint) {
    int ret = ioctl(fd, USBDEVFS_CLEAR_HALT, &endpoint);
    if(ret < 0) {
        LOG_E(TAG, "ioctl USBDEVFS_CLEAR_HALT error %d, %s", errno, strerror(errno));
    }

    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}