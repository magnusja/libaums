/*
 * (C) Copyright 2016 mjahnen <github@mgns.tech>
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
package com.github.mjdev.libaums.storageprovider;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.util.LruCache;
import android.webkit.MimeTypeMap;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.partition.Partition;
import com.github.mjdev.libaums.storageprovider.util.ParcelFileDescriptorUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class UsbDocumentProvider extends DocumentsProvider {

    private static final String TAG = UsbDocumentProvider.class.getSimpleName();

    private static final String DOCUMENTS_AUTHORITY = "com.github.mjdev.libaums.storageprovider.documents";

    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.storageprovider.USB_PERMISSION";

    private static final String DIRECTORY_SEPERATOR = "/";
    private static final String ROOT_SEPERATOR = ":";

    /**
     * Default root projection: everything but Root.COLUMN_MIME_TYPES
     */
    private final static String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES};

    /**
     * Default document projection: everything but Document.COLUMN_ICON and Document.COLUMN_SUMMARY
     */
    private final static String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED};

    private class UsbPartition {
        UsbDevice device;
        FileSystem fileSystem;
    }

    private final Map<String, UsbPartition> mRoots = new HashMap<>();

    private final LruCache<String, UsbFile> mFileCache = new LruCache<>(100);

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        Log.d(TAG, "queryRoots()");

        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            UsbPartition usbPartition = root.getValue();
            FileSystem fileSystem = usbPartition.fileSystem;
            UsbFile rootDirectory = fileSystem.getRootDirectory();
            String volumeLabel = fileSystem.getVolumeLabel();

            String title;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UsbDevice usbDevice = usbPartition.device;
                title = usbDevice.getManufacturerName() + " " + usbDevice.getProductName();
            } else {
                title = getContext().getString(R.string.storage_root);
            }

            String documentId = getDocIdForFile(rootDirectory);

            Log.d(TAG, "add root " + documentId);

            final MatrixCursor.RowBuilder row = result.newRow();
            // These columns are required
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, root.getKey());
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, documentId);
            row.add(DocumentsContract.Root.COLUMN_TITLE, title);
            row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_LOCAL_ONLY |
                    DocumentsContract.Root.FLAG_SUPPORTS_CREATE |
                    DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
            row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_usb_black_24dp);
            // These columns are optional
            row.add(DocumentsContract.Root.COLUMN_SUMMARY, volumeLabel);
            row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, fileSystem.getFreeSpace());
            // Root.COLUMN_MIME_TYPE is another optional column and useful if you have multiple roots with different
            // types of mime types (roots that don't match the requested mime type are automatically hidden)
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        Log.d(TAG, "queryDocument() " + documentId);

        try {
            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
            includeFile(result, getFileForDocId(documentId));
            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        Log.d(TAG, "queryChildDocuments() " + parentDocumentId);

        try {
            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
            UsbFile parent = getFileForDocId(parentDocumentId);
            for (UsbFile child : parent.listFiles()) {
                includeFile(result, child);
            }
            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        Log.d(TAG, "openDocument() " + documentId);

        try {
            UsbFile file = getFileForDocId(documentId);

            final int accessMode = ParcelFileDescriptor.parseMode(mode);
            if ((accessMode | ParcelFileDescriptor.MODE_READ_ONLY) == ParcelFileDescriptor.MODE_READ_ONLY) {
                Log.d(TAG, "openDocument() piping to UsbFileInputStream");
                return ParcelFileDescriptorUtil.pipeFrom(new UsbFileInputStream(file));
            } else if ((accessMode | ParcelFileDescriptor.MODE_WRITE_ONLY) == ParcelFileDescriptor.MODE_WRITE_ONLY) {
                Log.d(TAG, "openDocument() piping to UsbFileOutputStream");
                return ParcelFileDescriptorUtil.pipeTo(new UsbFileOutputStream(file));
            }

            Log.d(TAG, "openDocument() return null");


            return null;

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return documentId.startsWith(parentDocumentId);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        Log.d(TAG, "createDocument() " + parentDocumentId);

        try {
            UsbFile parent = getFileForDocId(parentDocumentId);

            UsbFile child;
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                child = parent.createDirectory(displayName);
            } else {
                child = parent.createFile(getFileName(mimeType, displayName));
            }

            return getDocIdForFile(child);

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName)
            throws FileNotFoundException {
        Log.d(TAG, "renameDocument() " + documentId);

        try {
            UsbFile file = getFileForDocId(documentId);
            file.setName(getFileName(getMimeType(file), displayName));
            mFileCache.remove(documentId);

            return getDocIdForFile(file);

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        Log.d(TAG, "deleteDocument() " + documentId);

        try {
            UsbFile file = getFileForDocId(documentId);
            file.delete();
            mFileCache.remove(documentId);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getDocumentType(String documentId) {
        Log.d(TAG, "getDocumentType() " + documentId);

        try {
            return getMimeType(getFileForDocId(documentId));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return "application/octet-stream";
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate()");

        Context context = getContext();
        assert context != null;

        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    discoverDevice(device);
                }
            }
        }, new IntentFilter(ACTION_USB_PERMISSION));

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                discoverDevice(device);
            }
        }, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                detachDevice(device);
            }
        }, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            discoverDevice(device);
        }

        return true;
    }

    private static String getMimeType(UsbFile file) {

        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName()).toLowerCase();
            if (extension != null) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                Log.d(TAG, "mimeType: " + mimeType);
                return mimeType;
            }
        }
        return "application/octet-stream";
    }

    private static String getFileName(String mimeType, String displayName) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(displayName).toLowerCase();
        if ((extension == null) ||
                !Objects.equals(mimeType, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                displayName = displayName + "." + extension;
            }
        }
        return displayName;
    }

    private void includeFile(final MatrixCursor result, final UsbFile file) throws FileNotFoundException {

        final MatrixCursor.RowBuilder row = result.newRow();

        // These columns are required
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getDocIdForFile(file));
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.isRoot() ? "" : file.getName());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file));

        int flags = DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                | DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                | DocumentsContract.Document.FLAG_SUPPORTS_RENAME;

        if (file.isDirectory()) {
            flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
        }

        /*
        // We only show thumbnails for image files - expect a call to openDocumentThumbnail for each file that has
        // this flag set
        if (mimeType.startsWith("image/"))
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;
            */
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        // COLUMN_SIZE is required, but can be null
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.isDirectory() ? 0 : file.getLength());
        // These columns are optional
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.isRoot() ? 0 : file.lastModified());
        // Document.COLUMN_ICON can be a resource id identifying a custom icon. The system provides default icons
        // based on mime type
        // Document.COLUMN_SUMMARY is optional additional information about the file
    }

    private void discoverDevice(UsbDevice device) {
        Log.d(TAG, "discoverDevice() " + device.toString());

        Context context = getContext();
        assert context != null;

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        for (UsbMassStorageDevice massStorageDevice : UsbMassStorageDevice.getMassStorageDevices(getContext())) {
            if (device.equals(massStorageDevice.getUsbDevice())) {
                if (usbManager.hasPermission(device)) {
                    addRoot(massStorageDevice);
                } else {
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                            ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
    }

    private void detachDevice(UsbDevice usbDevice) {
        Log.d(TAG, "detachDevice() " + usbDevice.toString());

        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            if (root.getValue().device.equals(usbDevice)) {
                Log.d(TAG, "remove rootId " + root.getKey());
                mRoots.remove(root.getKey());
                mFileCache.evictAll();
                notifyRootsChanged();
                break;
            }
        }
    }

    private void addRoot(UsbMassStorageDevice device) {
        Log.d(TAG, "addRoot() " + device.toString());

        try {
            device.init();
            for (Partition partition : device.getPartitions()) {
                UsbPartition usbPartition = new UsbPartition();
                usbPartition.device = device.getUsbDevice();
                usbPartition.fileSystem = partition.getFileSystem();
                mRoots.put(Integer.toString(partition.hashCode()), usbPartition);

                Log.d(TAG, "found root " + partition.hashCode());
            }
        } catch (IOException e) {
            Log.e(TAG, "error setting up device", e);
        }

        notifyRootsChanged();
    }

    private void notifyRootsChanged() {
        getContext().getContentResolver().notifyChange(
                DocumentsContract.buildRootsUri(DOCUMENTS_AUTHORITY), null, false);
    }

    private String getDocIdForFile(UsbFile file) throws FileNotFoundException {

        if (file.isRoot()) {
            for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
                if (file.equals(root.getValue().fileSystem.getRootDirectory())) {
                    String documentId = root.getKey() + ROOT_SEPERATOR;
                    mFileCache.put(documentId, file);
                    return documentId;
                }
            }
            throw new FileNotFoundException("Missing root entry");
        }

        String documentId = getDocIdForFile(file.getParent()) + DIRECTORY_SEPERATOR + file.getName();
        mFileCache.put(documentId, file);
        return documentId;
    }

    private UsbFile getFileForDocId(String documentId) throws IOException {
        Log.d(TAG, "getFileForDocId() " + documentId);

        UsbFile file = mFileCache.get(documentId);
        if (null != file)
            return file;

        Log.d(TAG, "No cache entry for " + documentId);

        final int splitIndex = documentId.lastIndexOf(DIRECTORY_SEPERATOR);
        if (splitIndex < 0) {
            String rootId = documentId.substring(0, documentId.length() - 1);
            UsbPartition usbPartition = mRoots.get(rootId);
            if (null == usbPartition) {
                throw new FileNotFoundException("Missing root for " + rootId);
            }

            file = usbPartition.fileSystem.getRootDirectory();
            mFileCache.put(documentId, file);
            return file;
        }

        UsbFile parent = getFileForDocId(documentId.substring(0, splitIndex));
        if (null == parent)
            throw new FileNotFoundException("Missing parent for " + documentId);

        String name = documentId.substring(splitIndex + 1);

        for (UsbFile child : parent.listFiles()) {
            if (name.equals(child.getName())) {
                mFileCache.put(documentId, child);
                return child;
            }
        }

        throw new FileNotFoundException("File not found " + documentId);
    }
}
