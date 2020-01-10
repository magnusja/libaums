#include <jni.h>
#include <libusb.h>
#include <stdio.h>

#include "log.h"

#define TAG "native_libusbcom"

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeInit(JNIEnv *env, jobject thiz, jint fd, jlongArray handle) {
    int ret = libusb_init(NULL);
    if (ret != 0) {
        LOG_E(TAG, "libusb_init returned %d", ret);
        return JNI_FALSE;
    }

    libusb_device_handle *devh = NULL;
    ret = libusb_wrap_sys_device(NULL, fd, &devh);
    if (ret != 0) {
        LOG_E(TAG, "libusb_wrap_sys_device returned %d", ret);
        return JNI_FALSE;
    }
    if (devh == NULL) {
        LOG_E(TAG, "libusb_wrap_sys_device device handle NULL");
        return JNI_FALSE;
    }

    jlong *body = (*env)->GetLongArrayElements(env, handle, NULL);
    // cache heap address in java class object
    body[0] = (jlong)devh;
    (*env)->ReleaseLongArrayElements(env, handle, body, NULL);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeClose(JNIEnv *env, jobject thiz, jlong handle) {
    libusb_close((libusb_device_handle*)(intptr_t)handle);
    libusb_exit(NULL);
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeBulkTransfer(JNIEnv *env, jobject thiz, jlong handle,
        jint endpointAddress, jbyteArray data, jint offset, jint length, jint timeout) {
    jbyte *c_data = (*env)->GetByteArrayElements(env, data, NULL);
    int transferred;
    int ret = libusb_bulk_transfer((libusb_device_handle*)(intptr_t)handle, (unsigned char)endpointAddress,
                                   (unsigned char *) &c_data[offset], length, &transferred, (unsigned int)timeout);

    (*env)->ReleaseByteArrayElements(env, data, c_data, NULL);

    if (ret == 0) {
        return transferred;
    } else {
        LOG_E(TAG, "libusb_bulk_transfer returned %d", ret);
        return ret;
    }
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeControlTransfer(JNIEnv *env, jobject thiz, jlong handle,
        jint requestType, jint request, jint value, int index, jbyteArray buffer, jint length, jint timeout) {
    jbyte *c_data = (*env)->GetByteArrayElements(env, buffer, NULL);
    int transferred;
    int ret = libusb_control_transfer((libusb_device_handle*)(intptr_t)handle, (uint8_t)requestType,
            (uint8_t)request, (uint8_t)value, (uint8_t)index, (unsigned char *)c_data,
            (uint16_t)length, (unsigned int)timeout);
    (*env)->ReleaseByteArrayElements(env, buffer, c_data, NULL);

    if (ret == 0) {
        return transferred;
    } else {
        LOG_E(TAG, "libusb_control_transfer returned %d", ret);
        return ret;
    }
}