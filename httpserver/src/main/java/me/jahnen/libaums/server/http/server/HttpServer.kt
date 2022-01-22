package me.jahnen.libaums.server.http.server

import me.jahnen.libaums.server.http.UsbFileProvider
import java.io.IOException

/**
 * Created by magnusja on 16/12/16.
 */
interface HttpServer {
    val isAlive: Boolean
    val hostname: String
    val listeningPort: Int
    var usbFileProvider: UsbFileProvider
    @Throws(IOException::class)
    fun start()

    @Throws(IOException::class)
    fun stop()
}

