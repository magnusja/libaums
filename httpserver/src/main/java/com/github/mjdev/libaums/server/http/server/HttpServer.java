package com.github.mjdev.libaums.server.http.server;

import com.github.mjdev.libaums.server.http.UsbFileHttpServer;
import com.github.mjdev.libaums.server.http.UsbFileProvider;

import java.io.IOException;

/**
 * Created by magnusja on 16/12/16.
 */
public interface HttpServer {
    void start() throws IOException;
    void stop() throws IOException;
    boolean isAlive();
    String getHostname();
    int getListeningPort();
    void setUsbFileProvider(UsbFileProvider provider);
}

