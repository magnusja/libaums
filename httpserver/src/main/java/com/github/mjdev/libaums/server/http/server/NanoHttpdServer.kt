package com.github.mjdev.libaums.server.http.server

import android.util.Log

import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.server.http.UsbFileProvider
import com.github.mjdev.libaums.server.http.exception.NotAFileException

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

import fi.iki.elonen.NanoHTTPD

/**
 * Created by magnusja on 16/12/16.
 */
class NanoHttpdServer : NanoHTTPD, HttpServer {
    private var usbFileProvider: UsbFileProvider? = null

    constructor(port: Int) : super(port) {}

    constructor(hostname: String, port: Int) : super(hostname, port) {}

    override fun setUsbFileProvider(provider: UsbFileProvider) {
        usbFileProvider = provider
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri: String
        try {
            uri = URLDecoder.decode(session.uri, "Unicode")
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "could not decode URL", e)

            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, "Unable to decode URL")
        }

        Log.d(TAG, "Request: $uri")

        val headers = session.headers
        val range = headers["range"]


        try {
            val fileToServe = usbFileProvider!!.determineFileToServe(uri)

            return range?.let { serveRangeOfFile(fileToServe, it) }
                    ?: serveCompleteFile(fileToServe)
        } catch (e: FileNotFoundException) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_HTML, e.message)
        } catch (e: NotAFileException) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, e.message)
        } catch (e: IOException) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_HTML, e.message)
        }

    }

    private fun serveCompleteFile(file: UsbFile): NanoHTTPD.Response {
        Log.d(TAG, "Serving complete file")

        val mimeType = NanoHTTPD.getMimeTypeForFile(file.name)

        val res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                mimeType, createInputStream(file), file.length)
        res.addHeader("Accept-Ranges", "bytes")

        return res
    }

    @Throws(IOException::class)
    private fun serveRangeOfFile(file: UsbFile, range: String): NanoHTTPD.Response {
        var range = range
        Log.d(TAG, "Serving range of file $range")

        val mimeType = NanoHTTPD.getMimeTypeForFile(file.name)

        var start: Long = 0
        var end: Long = -1
        val length = file.length

        if (range.startsWith("bytes=")) {
            range = range.substring("bytes=".length)
            val minus = range.indexOf('-')
            try {
                if (minus > 0) {
                    start = java.lang.Long.parseLong(range.substring(0, minus))
                    end = java.lang.Long.parseLong(range.substring(minus + 1))
                }
            } catch (e: NumberFormatException) {
                // ignore due to fallback values 0 and -1 -> length - 1
            }

        } else {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_HTML, "Range header invalid")
        }

        if (start < 0 || end >= length) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE,
                    NanoHTTPD.MIME_HTML, "Start < 0 or end >= actual length")
        }


        if (end < 0) {
            end = length - 1
        }

        var contentLength = end - start + 1
        if (contentLength < 0) {
            contentLength = 0
        }

        Log.d(TAG, "Serving file from $start to $end, Content-Length: $contentLength")

        val stream = createInputStream(file)
        stream.skip(start)

        val res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT,
                mimeType, stream, contentLength)
        res.addHeader("Accept-Ranges", "bytes")
        res.addHeader("Content-Length", "" + contentLength)
        res.addHeader("Content-Range", "bytes $start-$end/$length")

        return res
    }

    private fun createInputStream(file: UsbFile): InputStream {
        return UsbFileInputStream(file)
    }

    companion object {
        private val TAG = NanoHttpdServer::class.java.simpleName
    }
}
