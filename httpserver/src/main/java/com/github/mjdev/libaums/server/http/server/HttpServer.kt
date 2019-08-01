package com.github.mjdev.libaums.server.http.server

import com.github.mjdev.libaums.server.http.UsbFileHttpServer
import com.github.mjdev.libaums.server.http.UsbFileProvider

import java.io.IOException

/**
 * Created by magnusja on 16/12/16.
 */
interface HttpServer {
    val isAlive: Boolean
    val hostname: String
    val listeningPort: Int
    @Throws(IOException::class)
    fun start()

    @Throws(IOException::class)
    fun stop()

    fun setUsbFileProvider(provider: UsbFileProvider)
}

