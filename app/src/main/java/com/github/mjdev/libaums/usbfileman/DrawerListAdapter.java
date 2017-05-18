package com.github.mjdev.libaums.usbfileman;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.github.mjdev.libaums.UsbMassStorageDevice;

/**
 * Created by magnusja on 5/18/17.
 */

public class DrawerListAdapter extends ArrayAdapter<String> {
    UsbMassStorageDevice[] devices;
    public DrawerListAdapter(Context context, int resource, UsbMassStorageDevice[] devices) {
        super(context, resource);
        this.devices = devices;
    }

    @Nullable
    @Override
    public String getItem(int position) {
        String title;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsbDevice usbDevice = devices[position].getUsbDevice();
            title = usbDevice.getManufacturerName() + " " + usbDevice.getProductName();
        } else {
            title = getContext().getString(com.github.mjdev.libaums.storageprovider.R.string.storage_root);
        }
        return title;
    }

    @Override
    public int getCount() {
        return devices.length;
    }
}
