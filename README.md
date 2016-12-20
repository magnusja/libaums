libaums
=======

A library to access USB mass storage devices (pen drives, external HDDs, card readers) using the Android USB Host API. Currently it supports the SCSI command set and the FAT32 file system.

## Build status

**develop** [![Build Status](https://travis-ci.org/magnusja/libaums.svg?branch=develop)](https://travis-ci.org/magnusja/libaums)
**master** [![Build Status](https://travis-ci.org/magnusja/libaums.svg?branch=master)](https://travis-ci.org/magnusja/libaums)

## How to use

The library can be included into your project like this:

```
compile 'com.github.mjdev:libaums:0.4.0'
```

### Basics
#### Getting mass storage devices

```
UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this /* Activity */);

for(UsbDevice device: devices) {
    
    // before interacting with a device you need to call init()!
    device.init();
    
    // Only uses the first partition on the device
    FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
    Log.d(TAG, "Capacity: " + currentFs.getCapacity());
    Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
    Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
    Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());
}
```

#### Permissions

Your app needs to get permission from the user at run time to be able to communicate the device. From a `UsbMassStorageDevice` you can get the underlying `android.usb.UsbDevice` to do so.

```
PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Inten(ACTION_USB_PERMISSION), 0);
usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
```

For more information regarding permissions please check out the Android documentation: https://developer.android.com/guide/topics/connectivity/usb/host.html#permission-d

#### Working with files and folders

```
UsbFile root = currentFs.getRootDirectory();

UsbFile[] files = root.listFiles();
for(UsbFile file: files) {
    Log.d(TAG, file.getName());
    if(file.isDirectory()) {
        Log.d(TAG, file.getLength());
    }
}

UsbFile newDir = root.createDirectory("foo");
UsbFile file = newDir.createFile("bar.txt");

// write to a file
OutputStream os = new UsbFileOutputStream(file);

os.write("hello".getBytes());
os.close();

// read from a file
InputStream is = new UsbFileInputStream(file);
byte[] buffer = new byte[currentFs.getChunkSize()];
is.read(buffer);
```

#### Using buffered streams for more efficency

```
OutputStream os = UsbFileStreamFactory.createBufferedOutputStream(file, currentFs);
InputStream is = UsbFileStreamFactory.createBufferedInputStream(file, currentFs);
```

#### Provide files to external apps



### Hints

1. In the app/ directory you can find an example application using the library.
2. When copying a file always set the length via `UsbFile.setLength(long)` first. Otherwise the ClusterChain has to be increased for every call to write. This is very inefficent.
3. Always use FileSystem.getChunkSize() bytes as buffer size, because this alignes with the block sizes drives are using. Everything else is also most likeley a decrease in performance.
4. A good idea is to wrap the UsbFileInputStream/UsbFileOutputStream into BufferedInputStream/BufferedOutputStream
Libaums - Library to access USB Mass Storage Devices  
License: Apache 2.0 (see license.txt for details)

#### Thesis

Author: Magnus Jahnen, jahnen at in.tum.de  
Advisor: Nils Kannengießer, nils.kannengiesser at tum.de  
Supervisor: Prof. Uwe Baumgarten, baumgaru at in.tum.de  


Technische Universität München (TUM)  
Lehrstuhl/Fachgebiet für Betriebssysteme  
www.os.in.tum.de  

The library was developed by Mr. Jahnen as part of his bachelor's thesis in 2014. It's a sub-topic of the research topic "Secure Copy Protection for Mobile Apps" by Mr. Kannengießer. The full thesis document can be downloaded [here](https://www.os.in.tum.de/fileadmin/w00bdp/www/Lehre/Abschlussarbeiten/Jahnen-thesis.pdf).

We would appreciate an information email, when you plan to use the library in your projects.
