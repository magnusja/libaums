package com.github.mjdev.libaums

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.github.mjdev.libaums.UsbMassStorageDevice.Companion.getMassStorageDevices
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.UsbFile
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException


@LargeTest
class LibAumsTest {
    private val TAG: String = LibAumsTest::class.java.simpleName

    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    private val ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION"

    private lateinit var device: UsbMassStorageDevice
    private lateinit var fs: FileSystem
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setUp() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(noopBroadcastReceiver, filter)

        discoverDevice()
        setupDevice()
    }

    @Test
    @Throws(IOException::class)
    fun testCreateFile() {
        val root: UsbFile = fs.rootDirectory

        var testDirectory = root.search("testCreateFile")
        testDirectory?.delete()
        testDirectory = root.createDirectory("testCreateFile")

        var files = testDirectory.listFiles()
        Assert.assertTrue(files.isEmpty())
        testDirectory.createDirectory("testFile1")
        testDirectory.createDirectory("testFile2")

        files = testDirectory.listFiles()
        Assert.assertTrue(files.size == 2)
        testDirectory.delete()
    }

    @Test
    @Throws(IOException::class)
    fun testCreateDirectory() {
        val root: UsbFile = fs.rootDirectory
        var testDirectory = root.search("testCreateDirectory")
        testDirectory?.delete()
        testDirectory = root.createDirectory("testCreateDirectory")
        testDirectory.createDirectory("testDirectory1")
        testDirectory.createDirectory("testDirectory2")
        val files = testDirectory.listFiles()
        Assert.assertTrue(files.size == 2)
        testDirectory.delete()
    }

    /**
     * test fix for issue #36 in v0.3
     * search followed by createFile causes loss of all other files in same directory
     *
     *
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun testSearchAndCreateFile() {
        val root: UsbFile = fs.rootDirectory
        var testDirectory = root.search("testSearchAndCreateFile")
        testDirectory?.delete()

        // create non-empty directory
        testDirectory = root.createDirectory("testSearchAndCreateFile")
        testDirectory.createDirectory("testDirectory1")

        // search and create new sub-directory
        val searchDirectory = root.search("testSearchAndCreateFile")
        Assert.assertNotNull(searchDirectory)
        searchDirectory!!.createDirectory("testDirectory2")

        // count number of files in directory, should be two
        val files = searchDirectory.listFiles()
        Assert.assertTrue(files.size == 2)
        testDirectory.delete()
    }

    /**
     * test fix for issue #36 in v0.3
     * search followed by createDirectory causes loss of all other files in same directory
     *
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun testSearchAndCreateDirectory() {
        val root: UsbFile = fs.rootDirectory
        var testDirectory = root.search("testSearchAndCreateDirectory")
        testDirectory?.delete()

        // create non-empty directory
        testDirectory = root.createDirectory("testSearchAndCreateDirectory")
        testDirectory.createDirectory("testDirectory1")

        // search and create new sub-directory
        val searchDirectory = root.search("testSearchAndCreateDirectory")
        Assert.assertNotNull(searchDirectory)
        searchDirectory!!.createDirectory("testDirectory2")

        // count number of files in directory, should be two
        val files = searchDirectory.listFiles()
        Assert.assertTrue(files.size == 2)
        testDirectory.delete()
    }

    @Test
    @Throws(IOException::class)
    fun testdeleteAllFilesInDirectory() {
        val root: UsbFile = fs.rootDirectory
        var testDirectory = root.search("testCreateDirectory")
        testDirectory?.delete()
        testDirectory = root.createDirectory("testCreateDirectory")
        testDirectory.createDirectory("testDirectory1")
        testDirectory.createDirectory("testDirectory2")
        var files = testDirectory.listFiles()
        Assert.assertTrue(files.size == 2)
        for (file in files) {
            file.delete()
        }
        files = testDirectory.listFiles()
        Assert.assertTrue(files.isEmpty())
        testDirectory.delete()
    }

    /**
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    private fun discoverDevice() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = getMassStorageDevices(context)

        // we only use the first device
        device = devices[0]
        val usbDevice = device.usbDevice

        // The logged uppercase strings should always be present, so we grep for them in the QEMU
        // tests and we can auto-press the "Grant" button
        if (!usbManager.hasPermission(usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(
                    ACTION_USB_PERMISSION), 0)
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            Log.d(TAG, "Requested USB permission - USB-REQUESTED")
        } else {
            Log.d(TAG, "USB permission already granted - USB-GRANTED")
        }

        while (!usbManager.hasPermission(usbDevice)) {
            // Actively wait for permission since we don't have the luxury of waiting asynchronously
            Thread.sleep(500)
        }
    }

    /**
     * Sets the device up and shows the contents of the root directory.
     */
    private fun setupDevice() {
        try {
            device.init()
            fs = device.partitions[0].fileSystem
        } catch (e: IOException) {
            Log.e(TAG, "error setting up device", e)
        }
    }

    val noopBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // NOOP: in tests we need to poll for everything anyway, there's nothing to do here
        }
    }
}