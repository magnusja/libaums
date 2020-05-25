package com.github.mjdev.libaums.server.http.server

import android.util.Log
import com.github.mjdev.libaums.fs.UsbFileInputStream
import com.github.mjdev.libaums.server.http.UsbFileProvider
import com.github.mjdev.libaums.server.http.exception.NotAFileException
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import java.io.FileNotFoundException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder


/**
 * Created by magnusja on 16/12/16.
 */
class AsyncHttpServer(private val port: Int) : HttpServer, HttpServerRequestCallback {

    override lateinit var usbFileProvider: UsbFileProvider
    private val server = com.koushikdutta.async.http.server.AsyncHttpServer()
    override var isAlive = false
    override val hostname: String
        get() = ""
    override val listeningPort: Int
        get() = port

    init {

        server.get("/.*", this)
    }

    @Throws(IOException::class)
    override fun start() {
        server.listen(port)
        isAlive = true
    }

    @Throws(IOException::class)
    override fun stop() {
        server.stop()
        // force the server to stop even if there are ongoing connections
        AsyncServer.getDefault().stop()
        isAlive = false
    }

    override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        val uri: String
        try {
            uri = URLDecoder.decode(request.path, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "could not decode URL", e)
            response.code(404)
            response.send(e.message)
            return
        }

        Log.d(TAG, "Uri: $uri")

        try {
            val fileToServe = usbFileProvider.determineFileToServe(uri)
            response.sendStream(UsbFileInputStream(fileToServe), fileToServe.length)
        } catch (e: FileNotFoundException) {
            response.code(404)
            response.send(e.message)
        } catch (e: NotAFileException) {
            response.code(400)
            response.send(e.message)
        } catch (e: IOException) {
            response.code(500)
            response.send(e.message)
        }

    }

    companion object {
        private val TAG = AsyncHttpServer::class.java.simpleName
    }
}
