#include <jni.h>
#include <libusb.h>
#include <stdio.h>

#include "log.h"

#define TAG "native_libusbcom"

static void print_devs(libusb_device **devs)
{
    libusb_device *dev;
    int i = 0, j = 0;
    uint8_t path[8];

    while ((dev = devs[i++]) != NULL) {
        struct libusb_device_descriptor desc;
        int r = libusb_get_device_descriptor(dev, &desc);
        if (r < 0) {
            printf("failed to get device descriptor");
            return;
        }

        printf("%04x:%04x (bus %d, device %d)",
              desc.idVendor, desc.idProduct,
              libusb_get_bus_number(dev), libusb_get_device_address(dev));

        r = libusb_get_port_numbers(dev, path, sizeof(path));
        if (r > 0) {
            printf(" path: %d", path[0]);
            for (j = 1; j < r; j++)
                printf(".%d", path[j]);
        }
        printf("\n");
    }
}

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeInit(JNIEnv *env, jobject thiz) {
    int ret = libusb_init(NULL);

    if (ret == 0)
        return JNI_TRUE;
    else {
        LOG_E(TAG, "libusb_init returned %d", ret);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_me_jahnen_libaums_libusbcommunication_LibusbCommunication_nativeOpen(JNIEnv *env, jobject thiz) {
    LOG_D(TAG, "libusb open");
    libusb_device **devs;
    int count = libusb_get_device_list(NULL, &devs);
    if (count < 0){
        LOG_E(TAG, "libusb_get_device_list returned %d", count);
        return JNI_FALSE;
    }
    print_devs(devs);

    return JNI_TRUE;
}