package com.github.mjdev.libaums.server.http;

import android.support.annotation.NonNull;
import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;


import fi.iki.elonen.NanoHTTPD;

/**
 * This class allows to start a HTTP web server which can serve {@link com.github.mjdev.libaums.fs.UsbFile}s
 * to another app.
 *
 * For instance it can make an image available to the Web Browser without copying it to the internal
 * storage, or a video file to a video file as a HTTP stream.
 */
public class UsbFileHttpServer extends NanoHTTPD {

    private static final String TAG = UsbFile.class.getSimpleName();
    private UsbFile rootFile;

    public UsbFileHttpServer(String hostname, int port, @NonNull UsbFile file) {
        super(hostname, port);

        this.rootFile = file;
    }

    public UsbFileHttpServer(int port, @NonNull UsbFile file) {
        super(port);

        this.rootFile = file;
    }

    public UsbFileHttpServer(@NonNull UsbFile file) {
        super(8000);

        this.rootFile = file;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Reqeust: " + session.getUri());
        if(!rootFile.isDirectory()) {
            Log.d(TAG, "Serving root file");
            String mimeType = getMimeTypeForFile(rootFile.getName());

            Response res = newFixedLengthResponse(Response.Status.OK,
                    mimeType, new UsbFileInputStream(rootFile), rootFile.getLength());

            return res;
        }

        return super.serve(session);
    }

    public String getUrl() {
        return "http://" + getHostname() + ":" + getListeningPort();
    }
}
