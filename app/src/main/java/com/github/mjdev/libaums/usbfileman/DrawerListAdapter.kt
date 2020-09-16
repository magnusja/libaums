package com.github.mjdev.libaums.usbfileman

import android.content.Context
import android.os.Build
import android.widget.ArrayAdapter
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.storageprovider.R

/**
 * Created by magnusja on 5/18/17.
 */
class DrawerListAdapter(context: Context?, resource: Int, var devices: Array<UsbMassStorageDevice>) : ArrayAdapter<String?>(context!!, resource) {
    override fun getItem(position: Int): String? {
        val title: String
        title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usbDevice = devices[position].usbDevice
            usbDevice.manufacturerName + " " + usbDevice.productName
        } else {
            context.getString(R.string.storage_root)
        }
        return title
    }

    override fun getCount(): Int {
        return devices.size
    }
}