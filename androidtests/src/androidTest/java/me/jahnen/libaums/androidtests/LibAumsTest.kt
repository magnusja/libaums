package me.jahnen.libaums.androidtests

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import com.github.magnusja.libaums.javafs.JavaFsFileSystemCreator
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.UsbMassStorageDevice.Companion.getMassStorageDevices
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.FileSystemFactory
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.usb.UsbCommunicationFactory
import junit.framework.Assert
import junit.framework.TestCase
import me.jahnen.libaums.libusbcommunication.LibusbCommunicationCreator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.util.concurrent.TimeoutException

private const val USB_DISCOVER_TIMEOUT = 120 * 1000
private const val USB_PERMISSION_TIMEOUT = 120 * 1000

@RunWith(Parameterized::class)
@LargeTest
open class LibAumsTest(
        val underlyingUsbCommunication: UsbCommunicationFactory.UnderlyingUsbCommunication,
        private val usbCommName: String
) : TestCase("LibAumsTest $usbCommName") {

    private val TAG: String = LibAumsTest::class.java.simpleName

    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    private val ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION"

    private lateinit var device: UsbMassStorageDevice
    private lateinit var fs: FileSystem
    private val context = InstrumentationRegistry.getInstrumentation().context

    companion object {
        init {
            FileSystemFactory.registerFileSystem(JavaFsFileSystemCreator())
            UsbCommunicationFactory.registerCommunication(LibusbCommunicationCreator())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): List<Array<Any>> {
            return listOf(
                    arrayOf(UsbCommunicationFactory.UnderlyingUsbCommunication.USB_REQUEST_ASYNC, "USB_REQUEST_ASYNC"),
                    arrayOf(UsbCommunicationFactory.UnderlyingUsbCommunication.DEVICE_CONNECTION_SYNC, "DEVICE_CONNECTION_SYNC"),
                    arrayOf(UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER, "LIBUSB_COMM")
            )
        }
    }

    @Before
    public fun before() {
        println("Running test with communication: $usbCommName")
    }

    // Workaround for global before/after
    @Rule
    @JvmField
    public var resource: ExternalResource = object : ExternalResource() {
        override fun before() {
            UsbCommunicationFactory.underlyingUsbCommunication = underlyingUsbCommunication

            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            context.registerReceiver(noopBroadcastReceiver, filter)

            discoverDevice()
            setupDevice()
        }

        override fun after() {
            device.close()
        }
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
        lateinit var devices: Array<UsbMassStorageDevice>

        val startDiscoverTime = System.currentTimeMillis()
        Log.d(TAG, "Waiting for a USB mass storage device")
        do {
            devices = getMassStorageDevices(context)
            if (devices.isEmpty()) {
                Thread.sleep(1)
            }
        } while (devices.isEmpty() && System.currentTimeMillis() < startDiscoverTime + USB_DISCOVER_TIMEOUT)

        if (devices.isEmpty()) {
            throw TimeoutException("Timed out waiting for device")
        }

        // we only use the first device
        device = devices[0]
        val usbDevice = device.usbDevice

        Log.d(TAG, "Running tests with device ${usbDevice.deviceName}:  " +
                "${usbDevice.manufacturerName} ${usbDevice.productName} " +
                "(${usbDevice.vendorId.toString(16)}:${usbDevice.productId.toString(16)}")

        // The logged uppercase strings should always be present, so we grep for them in the QEMU
        // tests and we can auto-press the "Grant" button
        if (!usbManager.hasPermission(usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(
                    ACTION_USB_PERMISSION), 0)
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            Log.d(TAG, "Requested USB permission - USB-PERMISSION-REQUESTED")
        } else {
            Log.d(TAG, "USB permission already granted - USB-PERMISSION-GRANTED")
        }

        val startWaitPermissionTime = System.currentTimeMillis()
        while (!usbManager.hasPermission(usbDevice) && System.currentTimeMillis() < startWaitPermissionTime + USB_PERMISSION_TIMEOUT) {
            // Actively wait for permission since we don't have the luxury of waiting asynchronously
            Thread.sleep(500)
        }
        if (!usbManager.hasPermission(usbDevice)) {
            throw TimeoutException("Timed out waiting for permission to use USB device")
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
