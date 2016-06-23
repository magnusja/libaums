package com.github.mjdev.libaums.usbfileman;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Add activity to AndroidManifest.xml and start from there to run tests
 *
 * When debugging applications that use USB accessory or host features, you most likely will have
 * USB hardware connected to your Android-powered device. This will prevent you from having an adb
 * connection to the Android-powered device via USB. You can still access adb over a network
 * connection. To enable adb over a network connection:
 *
 * Connect the Android-powered device via USB to your computer.
 *
 * From your SDK platform-tools/ directory, enter adb tcpip 5555 at the command prompt.
 *
 * To get device-ip-address type 'adb shell ifconfig'
 *
 * Enter adb connect <device-ip-address>:5555 You should now be connected to the Android-powered
 * device and can issue the usual adb commands like adb logcat.
 *
 * To set your device to listen on USB, enter adb usb.
 *
 * Created by nowell on 26/04/16.
 */
// TODO: make this an actual Android unit test
public class LibAumsTest extends AppCompatActivity {

    private static final String TAG = LibAumsTest.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
        discoverDevice();
    }

    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";

    private UsbMassStorageDevice device;

    private FileSystem fs;

    /**
     * run all tests on the usb mass storage device
     */
    private void runAll() {

        try {

            testCreateFile();

            testCreateDirectory();

            testSearchAndCreateFile();

            testSearchAndCreateDirectory();

            testdeleteAllFilesInDirectory();

            Log.d(TAG, "PASS");

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "FAIL");
        }

    }

    private void testCreateFile()
            throws IOException {

        UsbFile[] files;

        UsbFile root = fs.getRootDirectory();

        UsbFile testDirectory = root.search("testCreateFile");
        if(testDirectory != null) {
            testDirectory.delete();
        }

        testDirectory = root.createDirectory("testCreateFile");
        files = testDirectory.listFiles();
        assertTrue((files != null) && (files.length == 0));

        testDirectory.createDirectory("testFile1");

        testDirectory.createDirectory("testFile2");

        files = testDirectory.listFiles();
        assertTrue((files != null) && (files.length == 2));

        testDirectory.delete();

    }

    private void testCreateDirectory()
            throws IOException {

        UsbFile root = fs.getRootDirectory();

        UsbFile testDirectory = root.search("testCreateDirectory");
        if(testDirectory != null) {
            testDirectory.delete();
        }

        testDirectory = root.createDirectory("testCreateDirectory");

        testDirectory.createDirectory("testDirectory1");

        testDirectory.createDirectory("testDirectory2");

        UsbFile[] files = testDirectory.listFiles();

        assertTrue((files != null) && (files.length == 2));

        testDirectory.delete();

    }

    /**
     * test fix for issue #36 in v0.3
     * search followed by createFile causes loss of all other files in same directory
     *
     *
     * @throws IOException
     */
    private void testSearchAndCreateFile()
            throws IOException {

        UsbFile root = fs.getRootDirectory();

        UsbFile testDirectory = root.search("testSearchAndCreateFile");
        if(testDirectory != null) {
            testDirectory.delete();
        }

        // create non-empty directory

        testDirectory = root.createDirectory("testSearchAndCreateFile");

        testDirectory.createDirectory("testDirectory1");

        // search and create new sub-directory

        UsbFile searchDirectory = root.search("testSearchAndCreateFile");
        assertNotNull(searchDirectory);

        searchDirectory.createDirectory("testDirectory2");

        // count number of files in directory, should be two

        UsbFile[] files = searchDirectory.listFiles();
        assertTrue((files != null) && (files.length == 2));

        testDirectory.delete();

    }

    /**
     * test fix for issue #36 in v0.3
     * search followed by createDirectory causes loss of all other files in same directory
     *
     * @throws IOException
     */
    private void testSearchAndCreateDirectory()
            throws IOException {

        UsbFile root = fs.getRootDirectory();

        UsbFile testDirectory = root.search("testSearchAndCreateDirectory");
        if(testDirectory != null) {
            testDirectory.delete();
        }

        // create non-empty directory

        testDirectory = root.createDirectory("testSearchAndCreateDirectory");

        testDirectory.createDirectory("testDirectory1");

        // search and create new sub-directory

        UsbFile searchDirectory = root.search("testSearchAndCreateDirectory");
        assertNotNull(searchDirectory);

        searchDirectory.createDirectory("testDirectory2");

        // count number of files in directory, should be two

        UsbFile[] files = searchDirectory.listFiles();
        assertTrue((files != null) && (files.length == 2));

        testDirectory.delete();

    }

    private void testdeleteAllFilesInDirectory()
            throws IOException {

        UsbFile root = fs.getRootDirectory();

        UsbFile testDirectory = root.search("testCreateDirectory");
        if(testDirectory != null) {
            testDirectory.delete();
        }

        testDirectory = root.createDirectory("testCreateDirectory");

        testDirectory.createDirectory("testDirectory1");

        testDirectory.createDirectory("testDirectory2");

        UsbFile[] files = testDirectory.listFiles();

        assertTrue((files != null) && (files.length == 2));


        for(UsbFile file : files) {
            file.delete();
        }

        files = testDirectory.listFiles();

        assertTrue((files != null) && (files.length == 0));

        testDirectory.delete();

    }

    /**
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    private void discoverDevice() {

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);

        // we only use the first device
        device = devices[0];

        UsbDevice usbDevice = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {

            Log.d(TAG, "received usb device via intent");
            // requesting permission is not needed in this case
            setupDevice();

        } else {

            // first request permission from user to communicate with the
            // underlying
            // UsbDevice
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                    ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(device.getUsbDevice(), permissionIntent);

        }

    }

    /**
     * Sets the device up and shows the contents of the root directory.
     */
    private void setupDevice() {

        try {

            device.init();

            fs = device.getPartitions().get(0).getFileSystem();

            runAll();

        } catch (IOException e) {
            Log.e(TAG, "error setting up device", e);
        }

    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if (device != null) {
                        setupDevice();
                    }

                }

            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device attached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    discoverDevice();
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(TAG, "USB device detached");

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    if (LibAumsTest.this.device != null) {
                        LibAumsTest.this.device.close();
                    }
                    // check if there are other devices or set action bar title
                    // to no device if not
                    discoverDevice();
                }

            }

        }
    };

}
