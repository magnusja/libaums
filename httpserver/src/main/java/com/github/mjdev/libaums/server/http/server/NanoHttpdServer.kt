package com.github.mjdev.libaums.server.http.server

import android.util.Log
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.server.http.UsbFileProvider
import com.github.mjdev.libaums.server.http.exception.NotAFileException
import fi.iki.elonen.NanoHTTPD
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * Created by magnusja on 16/12/16.
 */
class NanoHttpdServer : HttpServer {

    override val isAlive: Boolean
        get() = httpServer.isAlive
    override val hostname: String
        get() = httpServer.hostname
    override val listeningPort: Int
        get() = httpServer.listeningPort

    private inner class NHttpServer : NanoHTTPD {

        constructor(port: Int) : super(port)
        constructor(hostname: String, port: Int) : super(hostname, port)

        override fun serve(session: IHTTPSession): Response {
            val uri: String
            try {
                uri = URLDecoder.decode(session.uri, "Unicode")
            } catch (e: UnsupportedEncodingException) {
                Log.e(TAG, "could not decode URL", e)

                return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        MIME_HTML, "Unable to decode URL")
            }

            Log.d(TAG, "Request: $uri")

            val headers = session.headers
            val range = headers["range"]


            return try {
                val fileToServe = usbFileProvider.determineFileToServe(uri)

                range?.let { serveRangeOfFile(fileToServe, it) }
                        ?: serveCompleteFile(fileToServe)
            } catch (e: FileNotFoundException) {
                newFixedLengthResponse(Response.Status.NOT_FOUND,
                        MIME_HTML, e.message)
            } catch (e: NotAFileException) {
                newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        MIME_HTML, e.message)
            } catch (e: IOException) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        MIME_HTML, e.message)
            }

        }

        private fun serveCompleteFile(file: UsbFile): Response {
            Log.d(TAG, "Serving complete file")

            val mimeType = getMimeTypeForFile(file.name)

            val res = newFixedLengthResponse(Response.Status.OK,
                    mimeType, createInputStream(file), file.length)
            res.addHeader("Accept-Ranges", "bytes")

            return res
        }

        @Throws(IOException::class)
        private fun serveRangeOfFile(file: UsbFile, range: String): Response {
            var newRange = range
            Log.d(TAG, "Serving range of file $newRange")

            val mimeType = getMimeTypeForFile(file.name)

            var start: Long = 0
            var end: Long = -1
            val length = file.length

            if (newRange.startsWith("bytes=")) {
                newRange = newRange.substring("bytes=".length)
                val minus = newRange.indexOf('-')
                try {
                    if (minus > 0) {
                        start = java.lang.Long.parseLong(newRange.substring(0, minus))
                        end = java.lang.Long.parseLong(newRange.substring(minus + 1))
                    }
                } catch (e: NumberFormatException) {
                    // ignore due to fallback values 0 and -1 -> length - 1
                }

            } else {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                        MIME_HTML, "Range header invalid")
            }

            if (start < 0 || end >= length) {
                return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                        MIME_HTML, "Start < 0 or end >= actual length")
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

            val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT,
                    mimeType, stream, contentLength)
            res.addHeader("Accept-Ranges", "bytes")
            res.addHeader("Content-Length", "" + contentLength)
            res.addHeader("Content-Range", "bytes $start-$end/$length")

            return res
        }

        private fun createInputStream(file: UsbFile): InputStream {
            return UsbFileInputStream(file)
        }
    }

    override lateinit var usbFileProvider: UsbFileProvider
    private var httpServer: NHttpServer

    constructor(port: Int) {
        httpServer = NHttpServer(port)
    }

    constructor(hostname: String, port: Int) {
        httpServer = NHttpServer(hostname, port)
    }

    override fun start() {
        httpServer.start()
    }

    override fun stop() {
        httpServer.stop()
    }

    companion object {
        private val TAG = NanoHttpdServer::class.java.simpleName
    }
}
