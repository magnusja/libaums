package com.github.mjdev.libaums.server.http.server;

import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.server.http.UsbFileProvider;
import com.github.mjdev.libaums.server.http.exception.NotAFileException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by magnusja on 16/12/16.
 */
public class NanoHttpdServer extends NanoHTTPD implements  HttpServer {
    private static final String TAG =  NanoHttpdServer.class.getSimpleName();
    private UsbFileProvider usbFileProvider;

    public NanoHttpdServer(int port) {
        super(port);
    }

    public NanoHttpdServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public void setUsbFileProvider(UsbFileProvider provider) {
        usbFileProvider = provider;
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


        try {
            UsbFile fileToServe = usbFileProvider.determineFileToServe(uri);

            if(range == null) {
                return serveCompleteFile(fileToServe);
            } else {
                return serveRangeOfFile(fileToServe, range);
            }
        } catch (FileNotFoundException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_HTML, e.getMessage());
        } catch (NotAFileException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, e.getMessage());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_HTML, e.getMessage());
        }
    }

    private Response serveCompleteFile(UsbFile file) {
        Log.d(TAG, "Serving complete file");

        String mimeType = getMimeTypeForFile(file.getName());

        Response res = newFixedLengthResponse(Response.Status.OK,
                mimeType, createInputStream(file), file.getLength());
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

        InputStream stream = createInputStream(file);
        stream.skip(start);

        Response res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT,
                mimeType, stream, contentLength);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Content-Length", "" + contentLength);
        res.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);

        return res;
    }

    private InputStream createInputStream(UsbFile file) {
        return new UsbFileInputStream(file);
    }
}
