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
        String uri = session.getUri();
        Log.d(TAG, "Request: " + uri);
        if(!rootFile.isDirectory()) {
            if(!"/".equals(uri)) {
                // return a 404
                return super.serve(session);
            }

            Log.d(TAG, "Serving root file");
            String mimeType = getMimeTypeForFile(rootFile.getName());

            Response res = newFixedLengthResponse(Response.Status.OK,
                    mimeType, new UsbFileInputStream(rootFile), rootFile.getLength());

            return res;
        }

        return super.serve(session);
    }

    public String getBaseUrl() {
        return "http://" + getHostname() + ":" + getListeningPort() + "/";
    }
}
