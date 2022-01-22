/*
 * (C) Copyright 2014-2016 mjahnen <github@mgns.tech>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package me.jahnen.libaums.core.usbfileman

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import me.jahnen.libaums.javafs.JavaFsFileSystemCreator
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.UsbMassStorageDevice.Companion.getMassStorageDevices
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.FileSystemFactory.registerFileSystem
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileInputStream
import me.jahnen.libaums.core.fs.UsbFileStreamFactory.createBufferedOutputStream
import me.jahnen.libaums.server.http.UsbFileHttpServerService
import me.jahnen.libaums.server.http.UsbFileHttpServerService.ServiceBinder
import me.jahnen.libaums.server.http.server.AsyncHttpServer
import me.jahnen.libaums.core.usb.UsbCommunicationFactory
import me.jahnen.libaums.core.usb.UsbCommunicationFactory.registerCommunication
import me.jahnen.libaums.core.usb.UsbCommunicationFactory.underlyingUsbCommunication
import me.jahnen.libaums.libusbcommunication.LibusbCommunicationCreator
import java.io.*
import java.nio.ByteBuffer
import java.util.*

/**
 * MainActivity of the demo application which shows the contents of the first
 * partition.
 *
 * @author mjahnen
 */
class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    companion object {
        /**
         * Action string to request the permission to communicate with an UsbDevice.
         */
        private const val ACTION_USB_PERMISSION = "me.jahnen.libaums.USB_PERMISSION"
        private val TAG = MainActivity::class.java.simpleName
        private const val COPY_STORAGE_PROVIDER_RESULT = 0
        private const val OPEN_STORAGE_PROVIDER_RESULT = 1
        private const val OPEN_DOCUMENT_TREE_RESULT = 2
        private const val REQUEST_EXT_STORAGE_WRITE_PERM = 0

        init {
            registerFileSystem(JavaFsFileSystemCreator())
            registerCommunication(LibusbCommunicationCreator())
            underlyingUsbCommunication = UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER
        }
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        setupDevice()
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.d(TAG, "USB device attached")

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    discoverDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                Log.d(TAG, "USB device detached")

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    if (currentDevice != -1) {
                        massStorageDevices[currentDevice].close()
                    }
                    // check if there are other devices or set action bar title
                    // to no device if not
                    discoverDevice()
                }
            }
        }
    }

    /**
     * Dialog to create new directories.
     *
     * @author mjahnen
     */
    class NewDirDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity as MainActivity
            return AlertDialog.Builder(activity).apply {
                setTitle("New Directory")
                setMessage("Please enter a name for the new directory")
                val input = EditText(activity)
                setView(input)
                setPositiveButton("Ok") { _, _ ->
                    val dir = activity.adapter.currentDir
                    try {
                        dir.createDirectory(input.text.toString())
                        activity.adapter.refresh()
                    } catch (e: Exception) {
                        Log.e(TAG, "error creating dir!", e)
                    }
                }
                setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                setCancelable(false)
            }.create()
        }
    }

    /**
     * Dialog to create new files.
     *
     * @author mjahnen
     */
    class NewFileDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity as MainActivity
            return AlertDialog.Builder(activity).apply {
                setTitle("New File")
                setMessage("Please enter a name for the new file and some input")
                val input = EditText(activity)
                val content = EditText(activity)
                setView(
                        LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL

                            addView(TextView(activity).apply { setText(R.string.name) })
                            addView(input)
                            addView(TextView(activity).apply { setText(R.string.content) })
                            addView(content)
                        }
                )
                setPositiveButton("Ok") { _, _ ->
                    val dir = activity.adapter.currentDir
                    try {
                        val file = dir.createFile(input.text.toString())
                        file.write(0, ByteBuffer.wrap(content.text.toString().toByteArray()))
                        file.close()
                        activity.adapter.refresh()
                    } catch (e: Exception) {
                        Log.e(TAG, "error creating file!", e)
                    }
                }
                setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                setCancelable(false)
            }.create()
        }
    }

    /**
     * Class to hold the files for a copy task. Holds the source and the
     * destination file.
     *
     * @author mjahnen
     */
    private class CopyTaskParam {
        /* package */
        var from: UsbFile? = null

        /* package */
        var to: File? = null
    }

    /**
     * Asynchronous task to copy a file from the mass storage device connected
     * via USB to the internal storage.
     *
     * @author mjahnen
     */
    private inner class CopyTask : AsyncTask<CopyTaskParam?, Int?, Void?>() {
        private val dialog: ProgressDialog = ProgressDialog(this@MainActivity).apply {
            setTitle("Copying file")
            setMessage("Copying a file to the internal storage, this can take some time!")
            isIndeterminate = false
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
        }
        private var param: CopyTaskParam? = null

        override fun onPreExecute() {
            dialog.show()
        }

        protected override fun doInBackground(vararg params: CopyTaskParam?): Void? {
            val time = System.currentTimeMillis()
            param = params[0]
            try {
                val out: OutputStream = BufferedOutputStream(FileOutputStream(param!!.to))
                val inputStream: InputStream = UsbFileInputStream(param!!.from!!)
                val bytes = ByteArray(currentFs.chunkSize)
                var count: Int
                var total: Long = 0
                Log.d(TAG, "Copy file with length: " + param!!.from!!.length)
                while (inputStream.read(bytes).also { count = it } != -1) {
                    out.write(bytes, 0, count)
                    total += count.toLong()
                    var progress = total.toInt()
                    if (param!!.from!!.length > Int.MAX_VALUE) {
                        progress = (total / 1024).toInt()
                    }
                    publishProgress(progress)
                }
                out.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "error copying!", e)
            }
            Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time))
            return null
        }

        override fun onPostExecute(result: Void?) {
            dialog.dismiss()
            val myIntent = Intent(Intent.ACTION_VIEW)
            val file = File(param!!.to!!.absolutePath)
            val extension = MimeTypeMap.getFileExtensionFromUrl(Uri
                    .fromFile(file).toString())
            val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    extension)
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                myIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                uri = FileProvider.getUriForFile(this@MainActivity,
                        this@MainActivity.applicationContext.packageName + ".provider",
                        file)
            } else {
                uri = Uri.fromFile(file)
            }
            myIntent.setDataAndType(uri, mimetype)
            try {
                startActivity(myIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this@MainActivity, "Could no find an app for that file!",
                        Toast.LENGTH_LONG).show()
            }
        }

        protected override fun onProgressUpdate(vararg values: Int?) {
            var max = param!!.from!!.length.toInt()
            if (param!!.from!!.length > Int.MAX_VALUE) {
                max = (param!!.from!!.length / 1024).toInt()
            }
            dialog.max = max
            dialog.progress = values[0]!!
        }
    }

    /**
     * Class to hold the files for a copy task. Holds the source and the
     * destination file.
     *
     * @author mjahnen
     */
    private class CopyToUsbTaskParam {
        /* package */
        var from: Uri? = null
    }

    /**
     * Asynchronous task to copy a file from the mass storage device connected
     * via USB to the internal storage.
     *
     * @author mjahnen
     */
    private inner class CopyToUsbTask : AsyncTask<CopyToUsbTaskParam?, Int?, Void?>() {
        private val dialog: ProgressDialog = ProgressDialog(this@MainActivity).apply {
            setTitle("Copying file")
            setMessage("Copying a file to the USB drive, this can take some time!")
            isIndeterminate = true
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
        }
        private var param: CopyToUsbTaskParam? = null
        private var name: String? = null
        private var size: Long = -1
        private fun queryUriMetaData(uri: Uri?) {
            contentResolver
                    .query(uri!!, null, null, null, null, null)
                    ?.apply {
                        if (moveToFirst()) {
                            // TODO: query created and modified times to write it USB
                            name = getString(
                                    getColumnIndex(OpenableColumns.DISPLAY_NAME))
                            Log.i(TAG, "Display Name: $name")
                            val sizeIndex = getColumnIndex(OpenableColumns.SIZE)
                            if (!isNull(sizeIndex)) {
                                size = getLong(sizeIndex)
                            }
                            Log.i(TAG, "Size: $size")
                        }
                    }
                    ?.close()
        }

        override fun onPreExecute() = dialog.show()

        protected override fun doInBackground(vararg params: CopyToUsbTaskParam?): Void? {
            val time = System.currentTimeMillis()
            param = params[0]
            queryUriMetaData(param!!.from)
            if (name == null) {
                val segments = param!!.from!!.path!!.split("/".toRegex()).toTypedArray()
                name = segments[segments.size - 1]
            }
            try {
                val file = adapter.currentDir.createFile(name!!)
                if (size > 0) {
                    file.length = size
                }
                val inputStream = contentResolver.openInputStream(param!!.from!!)
                val outputStream: OutputStream = createBufferedOutputStream(file, currentFs)
                val bytes = ByteArray(1337)
                var count: Int
                var total: Long = 0
                while (inputStream!!.read(bytes).also { count = it } != -1) {
                    outputStream.write(bytes, 0, count)
                    if (size > 0) {
                        total += count.toLong()
                        var progress = total.toInt()
                        if (size > Int.MAX_VALUE) {
                            progress = (total / 1024).toInt()
                        }
                        publishProgress(progress)
                    }
                }
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "error copying!", e)
            }
            Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time))
            return null
        }

        override fun onPostExecute(result: Void?) {
            dialog.dismiss()
            try {
                adapter.refresh()
            } catch (e: IOException) {
                Log.e(TAG, "Error refreshing adapter", e)
            }
        }

        protected override fun onProgressUpdate(vararg values: Int?) {
            dialog.isIndeterminate = false
            var max = size.toInt()
            if (size > Int.MAX_VALUE) {
                max = (size / 1024).toInt()
            }
            dialog.max = max
            dialog.progress = values[0]!!
        }
    }

    /**
     * Asynchronous task to copy a file from the mass storage device connected
     * via USB to the internal storage.
     *
     * @author mjahnen
     */
    private inner class CopyFolderToUsbTask : AsyncTask<CopyToUsbTaskParam?, Int?, Void?>() {
        private val dialog: ProgressDialog = ProgressDialog(this@MainActivity).apply {
            setTitle("Copying a folder")
            setMessage("Copying a folder to the USB drive, this can take some time!")
            isIndeterminate = true
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
        }
        private var param: CopyToUsbTaskParam? = null
        private var size: Long = -1
        private var pickedDir: DocumentFile? = null
        override fun onPreExecute() {
            dialog.show()
        }

        @Throws(IOException::class)
        private fun copyDir(dir: DocumentFile?, currentUsbDir: UsbFile) {
            for (file in dir!!.listFiles()) {
                Log.d(TAG, "Found file " + file.name + " with size " + file.length())
                if (file.isDirectory) {
                    copyDir(file, currentUsbDir.createDirectory(file.name!!))
                } else {
                    copyFile(file, currentUsbDir)
                }
            }
        }

        private fun copyFile(file: DocumentFile, currentUsbDir: UsbFile) {
            try {
                val usbFile = currentUsbDir.createFile(file.name!!)
                size = file.length()
                usbFile.length = file.length()
                val inputStream = contentResolver.openInputStream(file.uri)
                val outputStream: OutputStream = createBufferedOutputStream(usbFile, currentFs!!)
                val bytes = ByteArray(1337)
                var count: Int
                var total: Long = 0
                while (inputStream!!.read(bytes).also { count = it } != -1) {
                    outputStream.write(bytes, 0, count)
                    if (size > 0) {
                        total += count.toLong()
                        var progress = total.toInt()
                        if (file.length() > Int.MAX_VALUE) {
                            progress = (total / 1024).toInt()
                        }
                        publishProgress(progress)
                    }
                }
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "error copying!", e)
            }
        }

        protected override fun doInBackground(vararg params: CopyToUsbTaskParam?): Void? {
            val time = System.currentTimeMillis()
            param = params[0]
            pickedDir = DocumentFile.fromTreeUri(this@MainActivity, param!!.from!!)
            try {
                copyDir(pickedDir, adapter.currentDir.createDirectory(pickedDir!!.name!!))
            } catch (e: IOException) {
                Log.e(TAG, "could not copy directory", e)
            }
            Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time))
            return null
        }

        override fun onPostExecute(result: Void?) {
            dialog.dismiss()
            try {
                adapter.refresh()
            } catch (e: IOException) {
                Log.e(TAG, "Error refreshing adapter", e)
            }
        }

        protected override fun onProgressUpdate(vararg values: Int?) {
            dialog.apply {
                isIndeterminate = false
                max = size.toInt()
                if (size > Int.MAX_VALUE) {
                    max = (size / 1024).toInt()
                }
                progress = values[0]!!
            }
        }
    }

    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "on service connected $name")
            val binder = service as ServiceBinder
            serverService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "on service disconnected $name")
            serverService = null
        }
    }
    lateinit var listView: ListView
    lateinit var drawerListView: ListView
    lateinit var drawerLayout: DrawerLayout
    lateinit var drawerToggle: ActionBarDrawerToggle

    /* package */
    lateinit var adapter: UsbFileListAdapter
    private val dirs: Deque<UsbFile> = ArrayDeque()
    lateinit var currentFs: FileSystem
    lateinit var serviceIntent: Intent
    var serverService: UsbFileHttpServerService? = null
    lateinit var massStorageDevices: Array<UsbMassStorageDevice>
    private var currentDevice = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceIntent = Intent(this, UsbFileHttpServerService::class.java)
        setContentView(R.layout.activity_main)
        listView = findViewById<View>(R.id.listview) as ListView
        drawerListView = findViewById<View>(R.id.left_drawer) as ListView
        drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawerToggle = object : ActionBarDrawerToggle(
                this,  /* host Activity */
                drawerLayout,  /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close /* "close drawer" description */
        ) {
            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                supportActionBar!!.setTitle(massStorageDevices[currentDevice].partitions[0].volumeLabel)
            }

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                supportActionBar!!.setTitle("Devices")
            }
        }
        // Set the drawer toggle as the DrawerListener
        drawerLayout.addDrawerListener(drawerToggle)
        drawerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectDevice(position)
            drawerLayout.closeDrawer(drawerListView)
            drawerListView.setItemChecked(position, true)
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        listView.onItemClickListener = this
        registerForContextMenu(listView)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)
        discoverDevice()
    }

    override fun onStart() {
        super.onStart()
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    private fun selectDevice(position: Int) {
        currentDevice = position
        setupDevice()
    }

    /**
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    private fun discoverDevice() {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        massStorageDevices = getMassStorageDevices(this)
        if (massStorageDevices.isEmpty()) {
            Log.w(TAG, "no device found!")
            val actionBar = supportActionBar
            actionBar!!.title = "No device"
            listView.adapter = null
            return
        }
        drawerListView.adapter = DrawerListAdapter(this, R.layout.drawer_list_item, massStorageDevices)
        drawerListView.setItemChecked(0, true)
        currentDevice = 0
        val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "received usb device via intent")
            // requesting permission is not needed in this case
            setupDevice()
        } else {
            // first request permission from user to communicate with the underlying UsbDevice
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(
                    ACTION_USB_PERMISSION), 0)
            usbManager.requestPermission(massStorageDevices[currentDevice].usbDevice, permissionIntent)
        }
    }

    /**
     * Sets the device up and shows the contents of the root directory.
     */
    private fun setupDevice() {
        try {
            massStorageDevices[currentDevice].init()

            // we always use the first partition of the device
            currentFs = massStorageDevices[currentDevice].partitions[0].fileSystem.also {
                Log.d(TAG, "Capacity: " + it.capacity)
                Log.d(TAG, "Occupied Space: " + it.occupiedSpace)
                Log.d(TAG, "Free Space: " + it.freeSpace)
                Log.d(TAG, "Chunk size: " + it.chunkSize)
            }

            val root = currentFs.rootDirectory
            val actionBar = supportActionBar
            actionBar!!.title = currentFs.volumeLabel
            listView.adapter = UsbFileListAdapter(this, root).apply { adapter = this }

        } catch (e: IOException) {
            Log.e(TAG, "error setting up device", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val cl = MoveClipboard
        menu.findItem(R.id.paste).isEnabled = cl?.file != null
        menu.findItem(R.id.stop_http_server).isEnabled = serverService != null && serverService!!.isServerRunning
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true
        } else when (item.itemId) {
            R.id.create_file -> {
                NewFileDialog().show(fragmentManager, "NEW_FILE")
                true
            }
            R.id.create_dir -> {
                NewDirDialog().show(fragmentManager, "NEW_DIR")
                true
            }
            R.id.create_big_file -> {
                createBigFile()
                true
            }
            R.id.paste -> {
                move()
                true
            }
            R.id.stop_http_server -> {
                if (serverService != null) {
                    serverService!!.stopServer()
                }
                true
            }
            R.id.open_storage_provider -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (currentDevice != -1) {
                        Log.d(TAG, "Closing device first")
                        massStorageDevices[currentDevice].close()
                    }
                    val intent = Intent()
                    intent.action = Intent.ACTION_OPEN_DOCUMENT
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    val extraMimeTypes = arrayOf("image/*", "video/*")
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    startActivityForResult(intent, OPEN_STORAGE_PROVIDER_RESULT)
                }
                true
            }
            R.id.copy_from_storage_provider -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val intent = Intent()
                    intent.action = Intent.ACTION_OPEN_DOCUMENT
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    startActivityForResult(intent, COPY_STORAGE_PROVIDER_RESULT)
                }
                true
            }
            R.id.copy_folder_from_storage_provider -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val intent = Intent()
                    intent.action = Intent.ACTION_OPEN_DOCUMENT_TREE
                    startActivityForResult(intent, OPEN_DOCUMENT_TREE_RESULT)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val entry = adapter.getItem(info.id.toInt())
        return when (item.itemId) {
            R.id.delete_item -> {
                try {
                    entry!!.delete()
                    adapter.refresh()
                } catch (e: IOException) {
                    Log.e(TAG, "error deleting!", e)
                }
                true
            }
            R.id.rename_item -> {
                AlertDialog.Builder(this).apply {
                    setTitle("Rename")
                    setMessage("Please enter a name for renaming")
                    val input = EditText(this@MainActivity)
                    input.setText(entry!!.name)
                    setView(input)
                    setPositiveButton("Ok") { _, _ ->
                        try {
                            entry.name = input.text.toString()
                            adapter.refresh()
                        } catch (e: IOException) {
                            Log.e(TAG, "error renaming!", e)
                        }
                    }
                    setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    setCancelable(false)
                    create()
                    show()
                }
                true
            }
            R.id.move_item -> {
                val cl = MoveClipboard
                cl.file = entry
                true
            }
            R.id.start_http_server -> {
                startHttpServer(entry)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, rowId: Long) {
        val entry = adapter.getItem(position)
        try {
            if (entry.isDirectory) {
                dirs.push(adapter.currentDir)
                listView.adapter = UsbFileListAdapter(this, entry).also { adapter = it }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, R.string.request_write_storage_perm, Toast.LENGTH_LONG).show()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                REQUEST_EXT_STORAGE_WRITE_PERM)
                    }
                    return
                }
                val param = CopyTaskParam()
                param.from = entry
                val f = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    File(getExternalFilesDir(null)!!.absolutePath + "/usbfileman/cache")
                } else {
                    File(Environment.getExternalStorageDirectory().absolutePath + "/usbfileman/cache")
                }
                f.mkdirs()
                val index = if (entry.name.lastIndexOf(".") > 0) entry.name.lastIndexOf(".") else entry.name.length
                var prefix = entry.name.substring(0, index)
                val ext = entry.name.substring(index)
                // prefix must be at least 3 characters
                if (prefix.length < 3) {
                    prefix += "pad"
                }
                param.to = File.createTempFile(prefix, ext, f)
                CopyTask().execute(param)
            }
        } catch (e: IOException) {
            Log.e(TAG, "error starting to copy!", e)
        }
    }

    private fun startHttpServer(file: UsbFile?) {
        Log.d(TAG, "starting HTTP server")
        if (serverService == null) {
            Toast.makeText(this@MainActivity, "serverService == null!", Toast.LENGTH_LONG).show()
            return
        }
        if (serverService!!.isServerRunning) {
            Log.d(TAG, "Stopping existing server service")
            serverService!!.stopServer()
        }

        // now start the server
        try {
            serverService!!.startServer(file!!, AsyncHttpServer(8000))
            Toast.makeText(this@MainActivity, "HTTP server up and running", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(TAG, "Error starting HTTP server", e)
            Toast.makeText(this@MainActivity, "Could not start HTTP server", Toast.LENGTH_LONG).show()
        }
        if (file!!.isDirectory) {
            // only open activity when serving a file
            return
        }
        val myIntent = Intent(Intent.ACTION_VIEW)
        myIntent.data = Uri.parse(serverService!!.server!!.baseUrl + file.name)
        try {
            startActivity(myIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this@MainActivity, "Could no find an app for that file!",
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_EXT_STORAGE_WRITE_PERM -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == RESULT_OK) {
            Log.w(TAG, "Activity result is not ok")
            return
        }
        if (intent == null) {
            return
        }

        Log.i(TAG, "Uri: " + intent.data.toString())
        val newIntent = Intent(Intent.ACTION_VIEW).apply { data = intent.data }
        val params = CopyToUsbTaskParam().apply { from = intent.data }

        when (requestCode) {
            OPEN_STORAGE_PROVIDER_RESULT -> startActivity(newIntent)
            COPY_STORAGE_PROVIDER_RESULT -> CopyToUsbTask().execute(params)
            OPEN_DOCUMENT_TREE_RESULT -> CopyFolderToUsbTask().execute(params)
        }

    }

    /**
     * This methods creates a very big file for testing purposes. It writes only
     * a small chunk of bytes in every loop iteration, so the offset where the
     * write starts will not always be a multiple of the cluster or block size
     * of the file system or block device. As a plus the file has to be grown
     * after every loop iteration which tests for example on FAT32 the dynamic
     * growth of a cluster chain.
     */
    private fun createBigFile() {
        val dir = adapter.currentDir
        val file: UsbFile
        try {
            file = dir.createFile("big_file_test.txt")
            val outputStream: OutputStream = createBufferedOutputStream(file, currentFs)
            outputStream.write("START\n".toByteArray())
            var i: Int
            i = 6
            while (i < 9000) {
                outputStream.write("TEST\n".toByteArray())
                i += 5
            }
            outputStream.write("END\n".toByteArray())
            outputStream.close()
            adapter.refresh()
        } catch (e: IOException) {
            Log.e(TAG, "error creating big file!", e)
        }
    }

    /**
     * This method moves the file located in the [MoveClipboard] into the
     * current shown directory.
     */
    private fun move() {
        val cl = MoveClipboard
        val file = cl.file
        try {
            file?.moveTo(adapter.currentDir)
            adapter.refresh()
        } catch (e: IOException) {
            Log.e(TAG, "error moving!", e)
        }
        cl.file = null
    }

    override fun onBackPressed() {
        try {
            val dir = dirs.pop()
            listView.adapter = UsbFileListAdapter(this, dir).also { adapter = it }
        } catch (e: NoSuchElementException) {
            super.onBackPressed()
        } catch (e: IOException) {
            Log.e(TAG, "error initializing adapter!", e)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        if (!serverService!!.isServerRunning) {
            Log.d(TAG, "Stopping service")
            stopService(serviceIntent)
            if (currentDevice != -1) {
                Log.d(TAG, "Closing device")
                massStorageDevices[currentDevice].close()
            }
        }
    }
}