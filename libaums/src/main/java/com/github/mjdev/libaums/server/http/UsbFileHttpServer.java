package com.github.mjdev.libaums.server.http;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by mep on 04/02/16.
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
