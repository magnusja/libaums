#include <jni.h>
#include <sys/ioctl.h>
#include <linux/usbdevice_fs.h>

JNIEXPORT jboolean JNICALL
Java_com_github_mjdev_libaums_usb_AndroidUsbCommunication_resetUsbDeviceNative(JNIEnv *env, jobject thiz, jint fd) {
    int ret = ioctl(fd, USBDEVFS_RESET);
    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}