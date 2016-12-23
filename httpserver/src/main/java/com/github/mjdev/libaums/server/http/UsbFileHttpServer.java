/*
 * (C) Copyright 2016 mjahnen <jahnen@in.tum.de>
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

package com.github.mjdev.libaums.server.http;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.server.http.exception.NotAFileException;
import com.github.mjdev.libaums.server.http.server.HttpServer;


import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class allows to start a HTTP web server which can serve {@link com.github.mjdev.libaums.fs.UsbFile}s
 * to another app.
 *
 * For instance it can make an image available to the Web Browser without copying it to the internal
 * storage, or a video file to a video file as a HTTP stream.
 */
public class UsbFileHttpServer implements UsbFileProvider {

    private static final String TAG = UsbFileHttpServer.class.getSimpleName();
    private UsbFile rootFile;
    private LruCache<String, UsbFile> fileCache = new LruCache<>(100);
    private HttpServer server;

    public UsbFileHttpServer(@NonNull UsbFile file, @NonNull HttpServer server) {
        this.rootFile = file;
        this.server = server;
        server.setUsbFileProvider(this);
    }

    public String getBaseUrl() {
        String hostname = server.getHostname();
        if(hostname == null) {
            hostname = "localhost";
        }
        return "http://" + hostname + ":" + server.getListeningPort() + "/";
    }

    public void start() throws IOException {
        server.start();
    }

    public void stop() throws IOException {
        server.stop();
        fileCache.evictAll();
    }

    public boolean isAlive() {
        return server.isAlive();
    }

    public UsbFile determineFileToServe(String uri) throws IOException {
        UsbFile fileToServe = fileCache.get(uri);

        if (fileToServe == null) {
            Log.d(TAG, "Searching file on USB (URI: " + uri + ")");
            if(!rootFile.isDirectory()) {
                Log.d(TAG, "Serving root file");
                if(!"/".equals(uri) && !("/" + rootFile.getName()).equals(uri)) {
                    Log.d(TAG, "Invalid request, respond with 404");
                    throw new FileNotFoundException("Not found " + uri);
                }


                fileToServe = rootFile;
            } else {
                fileToServe = rootFile.search(uri.substring(1));
            }
        } else {
            Log.d(TAG, "Using lru cache for " + uri);
        }


        if(fileToServe == null) {
            Log.d(TAG, "fileToServe == null");
            throw new FileNotFoundException("Not found " + uri);
        }

        if(fileToServe.isDirectory()) {
            throw new NotAFileException();
        }

        fileCache.put(uri, fileToServe);

        return fileToServe;
    }
}
