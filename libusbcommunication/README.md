libusb module
=======

This module provides an alternative method of communication low level with the usb device. Instead of using [Android USB Host API](https://developer.android.com/guide/topics/connectivity/usb/host), it is using [libusb](https://github.com/libusb/libusb). This is a user space usb library which can be used on Android as well. USB communication is then established using Linux usbfs or udev. For more information please consult the libusb documentation.

libusb seems to provide more stable and reliable communication with the usb device compared to the API Android is providing. 


### Licensing

Unfortunately, libusb is licenced under LGPLv2.1 which might have some drawbacks if you are developing a closed source application.

The LGPL says 

```
You may convey a Combined Work under terms of your choice that […] if you also
do each of the following:
  a) [full attribution]
  b) [include a copy of the license]
  c) [if you display any copyright notices, you must mention the licensed Library]
  d) Do one of the following:
    0) [provide means for the user to rebuild or re-link your application against
       a modified version of the Library]
    1) [use runtime linking against a copy already present on the system, and allow
       the user to replace that copy]
  e) [provide clear instructions how to rebuild or re-link your application in light
     of the previous point]
```

where d(1) is not possible on Android, and d(0) only possible with serious overhead if you do not want to give people your source code. What is possible __in my opinion__ is that you offer people to provide you with a compiled binary of libusb and you include that back into your application which you sent back to the user. This is because of code signing issues. Another option would be to give poeple instructions on how to do code signing on their own and then offer an unsigned apk which contains everything except libusb.

__Note__ that this is only my personal understanding and not general advice how you should do it. Please conduct your own research on that topic, as __I cannot provide any guarantee that this information is correct!__

Refer to the following blog where someone claims that LPGL cannot be used in closed source applications under Android: https://xebia.com/blog/the-lgpl-on-android/

### How to use

#### Inclusion in your build.gradle

```ruby
implementation 'me.jahnen.libaums:libusbcommunication:0.3.0'
```

### Activate libusb communication

Before using libaums you have to tell it that you want to use libusb instead of the standard Android USB Host API:

```java
UsbCommunicationFactory.registerCommunication(new LibusbCommunicationCreator());
UsbCommunicationFactory.setUnderlyingUsbCommunication(UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER);
```

You want to do that in a static block in your first activity or the constructor before doing anything else. See also https://github.com/magnusja/libaums/blob/develop/app/src/main/java/com/github/mjdev/libaums/usbfileman/MainActivity.java#L104-L108

You need at least libaums v0.7.5 to be able to use the libusb module.

### Compile yourself

Download libusb and add the following line to your `local.properties`

```
libusb.dir=/Users/magnus/Documents/code/libusb-1.0.23
```
