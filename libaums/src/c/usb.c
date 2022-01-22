#include <jni.h>
#include <sys/ioctl.h>
#include <linux/usbdevice_fs.h>
#include <errno.h>
#include <string.h>
#include "../../../libusbcommunication/src/c/log.h"

#define TAG "native_usb_ioctl"

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_usb_AndroidUsbCommunication_resetUsbDeviceNative(JNIEnv *env, jobject thiz, jint fd) {
    int ret = ioctl(fd, USBDEVFS_RESET);
    if(ret < 0) {
        LOG_E(TAG, "ioctl USBDEVFS_RESET error %d, %s", errno, strerror(errno));
    }

    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_usb_AndroidUsbCommunication_clearHaltNative(JNIEnv *env, jobject thiz, jint fd, jint endpoint) {
    int ret = ioctl(fd, USBDEVFS_CLEAR_HALT, &endpoint);
    if(ret < 0) {
        LOG_E(TAG, "ioctl USBDEVFS_CLEAR_HALT error %d, %s", errno, strerror(errno));
    }

    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}