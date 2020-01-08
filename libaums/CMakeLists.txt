cmake_minimum_required(VERSION 3.4.1)

add_library( errno-lib
             SHARED
             src/c/errno.c )

add_library( usb-lib
             SHARED
             src/c/usb.c )