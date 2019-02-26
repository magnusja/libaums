libaums
=======
[![Javadocs](https://www.javadoc.io/badge/com.github.mjdev/libaums.svg)](https://www.javadoc.io/doc/com.github.mjdev/libaums)
[ ![Build Status](https://travis-ci.org/magnusja/libaums.svg?branch=develop)](https://travis-ci.org/magnusja/libaums)[ ![codecov](https://codecov.io/gh/magnusja/libaums/branch/develop/graph/badge.svg)](https://codecov.io/gh/magnusja/libaums)[ ![Download](https://api.bintray.com/packages/mjdev/maven/libaums/images/download.svg) ](https://bintray.com/mjdev/maven/libaums/_latestVersion)
[ ![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/libaums)

A library to access USB mass storage devices (pen drives, external HDDs, card readers) using the Android USB Host API. Currently it supports the SCSI command set and the FAT32 file system.

## How to use

### Install

The library can be included into your project like this:

```ruby
compile 'com.github.mjdev:libaums:0.6.0'
```

If you need the HTTP or the storage provider module:

```ruby
compile 'com.github.mjdev:libaums-httpserver:0.5.3'
compile 'com.github.mjdev:libaums-storageprovider:0.5.1'
```

### Basics
#### Getting mass storage devices

```java
UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this /* Context or Activity */);

for(UsbMassStorageDevice device: devices) {
    
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

```java
PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Inten(ACTION_USB_PERMISSION), 0);
usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
```

For more information regarding permissions please check out the Android documentation: https://developer.android.com/guide/topics/connectivity/usb/host.html#permission-d

#### Working with files and folders

```java
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

```java
OutputStream os = UsbFileStreamFactory.createBufferedOutputStream(file, currentFs);
InputStream is = UsbFileStreamFactory.createBufferedInputStream(file, currentFs);
```

#### Cleaning up

```java
// Don't forget to call UsbMassStorageDevice.close() when you are finished

device.close();
```

#### Provide access to external apps

Usually third party apps do not have access to the files on a mass storage device if the Android system does mount the device or this app integrates this library itself. To solve this issue there are two additional modules to provide access to other app. One uses the Storage Access Framework feature of Android (API level >= 19) and the other one spins up an HTTP server to allow downloading or streaming.

### Storage Access Framework
[![Javadocs](https://www.javadoc.io/badge/com.github.mjdev/libaums-storageprovider.svg)](https://www.javadoc.io/doc/com.github.mjdev/libaums-storageprovider)

To learn more about this visit: https://developer.android.com/guide/topics/providers/document-provider.html

To integrate this module in your app the only thing you have to do is add the definition in your AndroidManifest.xml.

```xml
<provider
    android:name="com.github.mjdev.libaums.storageprovider.UsbDocumentProvider"
    android:authorities="com.github.mjdev.libaums.storageprovider.documents"
    android:exported="true"
    android:grantUriPermissions="true"
    android:permission="android.permission.MANAGE_DOCUMENTS"
    android:enabled="@bool/isAtLeastKitKat">
    <intent-filter>
        <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
    </intent-filter>
</provider>
```

After that apps using the Storage Access Framework will be able to access the files of the USB mass storage device.

### HTTP server
[![Javadocs](https://www.javadoc.io/badge/com.github.mjdev/libaums-httpserver.svg)](https://www.javadoc.io/doc/com.github.mjdev/libaums-httpserver)

libaums currently supports two different HTTP server libraries.

1. [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
2. [AsyncHttpServer](https://github.com/koush/AndroidAsync/blob/master/AndroidAsync/src/com/koushikdutta/async/http/server/AsyncHttpServer.java)

You can spin up a server pretty easy, you just have to decide for a HTTP server implementation. If you do not have special requirements, you can just go for one, it should not make much of a difference.

```java
UsbFile file = ... // can be directory or file

HttpServer server = AsyncHttpServer(8000); // port 8000
// or
HttpServer server = NanoHttpdServer(8000); // port 8000

UsbFileHttpServer fileServer = new UsbFileHttpServer(file, server);
fileServer.start();
```

The file you privde can either be an actual file or a directory:

1. File: Accessible either via "/" or "/FILE_NAME"
2. Directory: All files in this directory und sub directories are accessable via their names. Directory listing is not supported!

If you want to be able to access these files when your app is in background, you should implement a service for that. There is an example available in the `httpserver` module. You can use it, but should subclass it or create your own to adapt it to your needs.

```java
private UsbFileHttpServerService serverService;

ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "on service connected " + name);
        UsbFileHttpServerService.ServiceBinder binder = (UsbFileHttpServerService.ServiceBinder) service;
        serverService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "on service disconnected " + name);
        serverService = null;
    }
};

@Override
protected void onCreate(Bundle savedInstanceState) {
...
    serviceIntent = new Intent(this, UsbFileHttpServerService.class);
...
}

 @Override
protected void onStart() {
    super.onStart();

    startService(serviceIntent);
    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
}

private void startHttpServer(final UsbFile file) {
...
    serverService.startServer(file, new AsyncHttpServer(8000));
...
}
```

See the example app for additional details on that.


#### Hints

1. In the `app/` directory you can find an example application using the library.
2. When copying a file always set the length via `UsbFile.setLength(long)` first. Otherwise the ClusterChain has to be increased for every call to write. This is very inefficent.
3. Always use `FileSystem.getChunkSize()` bytes as buffer size, because this alignes with the block sizes drives are using. Everything else is also most likeley a decrease in performance.
4. A good idea is to wrap the UsbFileInputStream/UsbFileOutputStream into BufferedInputStream/BufferedOutputStream. Also see `UsbFileStreamFactory`.

##### Thesis

Libaums - Library to access USB Mass Storage Devices  
License: Apache 2.0 (see license.txt for details)
Author: Magnus Jahnen, jahnen at in.tum.de  
Advisor: Nils Kannengießer, nils.kannengiesser at tum.de  
Supervisor: Prof. Uwe Baumgarten, baumgaru at in.tum.de  


Technische Universität München (TUM)  
Lehrstuhl/Fachgebiet für Betriebssysteme  
www.os.in.tum.de  

The library was developed by Mr. Jahnen as part of his bachelor's thesis in 2014. It's a sub-topic of the research topic "Secure Copy Protection for Mobile Apps" by Mr. Kannengießer. The full thesis document can be downloaded [here](https://www.os.in.tum.de/fileadmin/w00bdp/www/Lehre/Abschlussarbeiten/Jahnen-thesis.pdf).

We would appreciate an information email, when you plan to use the library in your projects.
