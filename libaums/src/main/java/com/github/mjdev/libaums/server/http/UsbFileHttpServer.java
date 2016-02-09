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


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

    public String getBaseUrl() {
        String hostname = getHostname();
        if(hostname == null) {
            hostname = "localhost";
        }
        return "http://" + hostname + ":" + getListeningPort() + "/";
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri;
        try {
            uri = URLDecoder.decode(session.getUri(), "Unicode");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "could not decode URL", e);

            return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, "Unable to decode URL");
        }
        Log.d(TAG, "Request: " + uri);

        Map<String, String> headers = session.getHeaders();
        String range = headers.get("range");

        UsbFile fileToServe;

        if(!rootFile.isDirectory()) {
            Log.d(TAG, "Serving root file");
            if(!"/".equals(uri) && !("/" + rootFile.getName()).equals(uri)) {
                Log.d(TAG, "Invalid request, respond with 404");
                // return a 404
                return super.serve(session);
            }


            fileToServe = rootFile;
        } else {
            try {
                fileToServe = rootFile.search(uri.substring(1));
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                return newFixedLengthResponse(Response.Status.FORBIDDEN,
                        NanoHTTPD.MIME_HTML, "IOException");
            }
        }

        if(fileToServe == null) {
            Log.d(TAG, "fileToServe == null");

            // return a 404
            return super.serve(session);
        }

        if(fileToServe.isDirectory()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, "Directory listing not supported");
        }

        if(range == null) {
            return serveCompleteFile(fileToServe);
        } else {
            try {
                return serveRangeOfFile(fileToServe, range);
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                return newFixedLengthResponse(Response.Status.FORBIDDEN,
                        NanoHTTPD.MIME_HTML, "IOException");
            }
        }
    }

    private Response serveCompleteFile(UsbFile file) {
        Log.d(TAG, "Serving complete file");

        String mimeType = getMimeTypeForFile(file.getName());

        Response res = newFixedLengthResponse(Response.Status.OK,
                mimeType, new UsbFileInputStream(file), file.getLength());
        res.addHeader("Accept-Ranges", "bytes");

        return res;
    }

    private Response serveRangeOfFile(UsbFile file, String range) throws IOException {
        Log.d(TAG, "Serving range of file " + range);

        String mimeType = getMimeTypeForFile(file.getName());

        long start = 0;
        long end = -1;
        long length = file.getLength();

        if (range.startsWith("bytes=")) {
            range = range.substring("bytes=".length());
            int minus = range.indexOf('-');
            try {
                if (minus > 0) {
                    start = Long.parseLong(range.substring(0, minus));
                    end = Long.parseLong(range.substring(minus + 1));
                }
            } catch (NumberFormatException e) {
                // ignore due to fallback values 0 and -1 -> length - 1
            }
        } else {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, "Range header invalid");
        }

        if(start < 0 || end >= length) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                    NanoHTTPD.MIME_HTML, "Start < 0 or end >= actual length");
        }


        if (end < 0) {
            end = length - 1;
        }

        long contentLength = end - start + 1;
        if (contentLength < 0) {
            contentLength = 0;
        }

        Log.d(TAG, "Serving file from " + start + " to " + end + ", Content-Length: " + contentLength);

        UsbFileInputStream stream = new UsbFileInputStream(file);
        stream.skip(start);

        Response res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT,
                mimeType, stream, contentLength);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Content-Length", "" + contentLength);
        res.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);

        return res;
    }
}
