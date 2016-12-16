package com.github.mjdev.libaums.server.http.server;

import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.server.http.UsbFileProvider;
import com.github.mjdev.libaums.server.http.exception.NotAFileException;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.FileNotFoundException;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by magnusja on 16/12/16.
 */
public class AsyncHttpServer implements HttpServer, HttpServerRequestCallback {
    private static final String TAG = AsyncHttpServer.class.getSimpleName();

    private UsbFileProvider usbFileProvider;
    private com.koushikdutta.async.http.server.AsyncHttpServer server =
            new com.koushikdutta.async.http.server.AsyncHttpServer();
    private boolean isAlive = false;
    private int port;

    public AsyncHttpServer(int port) {
        this.port = port;

        server.get("/", this);
    }

    @Override
    public void start() throws IOException {
        server.listen(5000);
        isAlive = true;
    }

    @Override
    public void stop() throws IOException {
        server.stop();
        isAlive = false;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public String getHostname() {
        return null;
    }

    @Override
    public int getListeningPort() {
        return port;
    }

    @Override
    public void setUsbFileProvider(UsbFileProvider provider) {
        usbFileProvider = provider;
    }

    @Override
    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
        String uri = request.getPath();
        Log.d(TAG, "Uri: " + uri);

        try {
            UsbFile fileToServe = usbFileProvider.determineFileToServe(uri);
            response.sendStream(new UsbFileInputStream(fileToServe), fileToServe.getLength());
        } catch (FileNotFoundException e) {
            response.code(404);
            response.send(e.getMessage());
        } catch (NotAFileException e) {
            response.code(400);
            response.send(e.getMessage());
        } catch (IOException e) {
            response.code(500);
            response.send(e.getMessage());
        }
    }
}
