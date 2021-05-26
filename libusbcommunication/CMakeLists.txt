cmake_minimum_required(VERSION 3.4.1)

message("Using libusb path ${LIBUSB_PATH}")

set(LIBUSB_SRC "${LIBUSB_PATH}/libusb")
set(LIBUSB_INCLUDE "${LIBUSB_PATH}")


include_directories(${LIBUSB_INCLUDE}/android/)  # config.h
include_directories(${LIBUSB_SRC})
add_library( libusb
             SHARED
             ${LIBUSB_SRC}/core.c
             ${LIBUSB_SRC}/strerror.c
             ${LIBUSB_SRC}/descriptor.c
             ${LIBUSB_SRC}/hotplug.c
             ${LIBUSB_SRC}/io.c
             ${LIBUSB_SRC}/sync.c
             ${LIBUSB_SRC}/os/linux_usbfs.c
             ${LIBUSB_SRC}/os/poll_posix.c
             ${LIBUSB_SRC}/os/threads_posix.c
             ${LIBUSB_SRC}/os/linux_netlink.c)
target_link_libraries(libusb log)


include_directories(${LIBUSB_INCLUDE})
add_library( libusbcom
             SHARED
             src/c/usb.c )
target_link_libraries(libusbcom libusb log)
