package com.github.mjdev.libaums.server.http;

import fi.iki.elonen.NanoHTTPD;

/**
 * This class allows to start a HTTP web server which can serve {@link com.github.mjdev.libaums.fs.UsbFile}s
 * to another app.
 *
 * For instance it can make an image available to the Web Browser without copying it to the internal
 * storage, or a video file to a video file as a HTTP stream.
 */
public class UsbFileHttpServer extends NanoHTTPD {

    public UsbFileHttpServer(String hostname, int port) {
        super(hostname, port);
    }

    public UsbFileHttpServer(int port) {
        super(port);
    }

    public UsbFileHttpServer() {
        super(8000);
    }
}
