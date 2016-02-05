package com.github.mjdev.libaums.server.http;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;

import java.util.HashMap;
import java.util.Map;

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

            Response res = new Response(Response.Status.OK, mimeType, new UsbFileInputStream(rootFile));
            res.addHeader("Content-Length", "" + rootFile.getLength());
            return res;
        }

        return super.serve(session);
    }

    private String getMimeTypeForFile(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileName.substring(dot + 1).toLowerCase());
        }
        return mime == null ? "application/octet-stream" : mime;
    }
}
