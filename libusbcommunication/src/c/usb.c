#include <jni.h>
#include <libusb.h>
#include <stdio.h>

#include "log.h"

#define TAG "native_libusbcom"

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeInit(JNIEnv *env, jobject thiz, jint fd, jlongArray handle) {
    LOG_D(TAG, "init native libusb");
    int ret;

// see https://github.com/magnusja/libaums/pull/335/files
// for 1.0.25 LIBUSB_OPTION_NO_DEVICE_DISCOVERY is same as LIBUSB_OPTION_WEAK_AUTHORITY
// essentially avoids enumerating USB devices which might cause trouble on non rooted devices
#if defined(LIBUSB_API_VERSION) && (LIBUSB_API_VERSION >= 0x01000108)
    ret = libusb_set_option(NULL, LIBUSB_OPTION_WEAK_AUTHORITY);
    if (ret != 0) {
        LOG_E(TAG, "libusb_set_option returned %d, %s", ret, libusb_strerror(ret));
        return ret;
    }
#endif

    ret = libusb_init(NULL);
    if (ret != 0) {
        LOG_E(TAG, "libusb_init returned %d, %s", ret, libusb_strerror(ret));
        return ret;
    }

    libusb_device_handle *devh = NULL;
    ret = libusb_wrap_sys_device(NULL, fd, &devh);
    if (ret != 0) {
        LOG_E(TAG, "libusb_wrap_sys_device returned %d, %s", ret, libusb_strerror(ret));
        return ret;
    }
    if (devh == NULL) {
        LOG_E(TAG, "libusb_wrap_sys_device device handle, %s NULL", libusb_strerror(ret));
        return LIBUSB_ERROR_OTHER;
    }

    jlong *body = (*env)->GetLongArrayElements(env, handle, NULL);
    // cache heap address in java class object
    body[0] = (jlong)devh;
    (*env)->ReleaseLongArrayElements(env, handle, body, NULL);

    return 0;
}

JNIEXPORT void JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeClose(JNIEnv *env, jobject thiz, jlong handle, jint interfaceNumber) {
    LOG_D(TAG, "close native libusb");
    //libusb_release_interface((libusb_device_handle*)(intptr_t)handle, interfaceNumber);
    libusb_close((libusb_device_handle*)(intptr_t)handle);
    libusb_exit(NULL);
}


JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeClaimInterface(JNIEnv *env, jobject thiz, jlong handle, jint interfaceNumber) {
    jint ret = libusb_detach_kernel_driver((libusb_device_handle*)(intptr_t)handle, interfaceNumber);
    if (ret != 0) {
        LOG_W(TAG, "libusb_detach_kernel_driver returned %d", ret);
    }

    ret = libusb_claim_interface((libusb_device_handle*)(intptr_t)handle, interfaceNumber);
    if (ret != 0) {
        LOG_E(TAG, "libusb_claim_interface returned %d, %s", ret, libusb_strerror(ret));
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeBulkTransfer(JNIEnv *env, jobject thiz, jlong handle,
        jint endpointAddress, jbyteArray data, jint offset, jint length, jint timeout) {
    jbyte *c_data = (*env)->GetByteArrayElements(env, data, NULL);
    jint transferred;
    jint ret = libusb_bulk_transfer((libusb_device_handle*)(intptr_t)handle, (unsigned char)endpointAddress,
                                   (unsigned char *) &c_data[offset], length, &transferred, (unsigned int)timeout);

    (*env)->ReleaseByteArrayElements(env, data, c_data, NULL);

    if (ret == 0) {
        return transferred;
    } else {
        LOG_E(TAG, "libusb_bulk_transfer returned %d, %s", ret, libusb_strerror(ret));
        return ret;
    }
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeControlTransfer(JNIEnv *env, jobject thiz, jlong handle,
        jint requestType, jint request, jint value, int index, jbyteArray buffer, jint length, jint timeout) {
    jbyte *c_data = (*env)->GetByteArrayElements(env, buffer, NULL);
    jint ret = libusb_control_transfer((libusb_device_handle*)(intptr_t)handle, (uint8_t)requestType,
            (uint8_t)request, (uint8_t)value, (uint8_t)index, (unsigned char *)c_data,
            (uint16_t)length, (unsigned int)timeout);
    (*env)->ReleaseByteArrayElements(env, buffer, c_data, NULL);

    return ret;
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeReset(JNIEnv *env, jobject thiz, jlong handle) {
    LOG_W(TAG, "libusb reset");
    return libusb_reset_device((libusb_device_handle*)(intptr_t)handle);
}

JNIEXPORT jint JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeClearHalt(JNIEnv *env, jobject thiz, jlong handle, jint endpointAddress) {
    LOG_W(TAG, "libusb clear halt");
    return libusb_clear_halt((libusb_device_handle*)(intptr_t)handle, (unsigned char) endpointAddress);
}
