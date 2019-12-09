/*
 * (C) Copyright 2016 mjahnen <github@mgns.tech>
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

package com.github.mjdev.libaums.server.http

import android.util.Log
import android.util.LruCache

import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.server.http.exception.NotAFileException
import com.github.mjdev.libaums.server.http.server.HttpServer


import java.io.FileNotFoundException
import java.io.IOException

/**
 * This class allows to start a HTTP web server which can serve [com.github.mjdev.libaums.fs.UsbFile]s
 * to another app.
 *
 * For instance it can make an image available to the Web Browser without copying it to the internal
 * storage, or a video file to a video file as a HTTP stream.
 */
class UsbFileHttpServer(private val rootFile: UsbFile, private val server: HttpServer) : UsbFileProvider {
    private val fileCache = LruCache<String, UsbFile>(100)

    val baseUrl: String
        get() {
            var hostname: String? = server.hostname
            if (hostname == null) {
                hostname = "localhost"
            }
            return "http://" + hostname + ":" + server.listeningPort + "/"
        }

    val isAlive: Boolean
        get() = server.isAlive

    init {
        server.usbFileProvider = this
    }

    @Throws(IOException::class)
    fun start() {
        server.start()
    }

    @Throws(IOException::class)
    fun stop() {
        server.stop()
        fileCache.evictAll()
    }

    @Throws(IOException::class)
    override fun determineFileToServe(uri: String): UsbFile {
        var fileToServe: UsbFile? = fileCache.get(uri)

        if (fileToServe == null) {
            Log.d(TAG, "Searching file on USB (URI: $uri)")
            if (!rootFile.isDirectory) {
                Log.d(TAG, "Serving root file")
                if ("/" != uri && "/" + rootFile.name != uri) {
                    Log.d(TAG, "Invalid request, respond with 404")
                    throw FileNotFoundException("Not found $uri")
                }


                fileToServe = rootFile
            } else {
                fileToServe = rootFile.search(uri.substring(1))
            }
        } else {
            Log.d(TAG, "Using lru cache for $uri")
        }


        if (fileToServe == null) {
            Log.d(TAG, "fileToServe == null")
            throw FileNotFoundException("Not found $uri")
        }

        if (fileToServe.isDirectory) {
            throw NotAFileException()
        }

        fileCache.put(uri, fileToServe)

        return fileToServe
    }

    companion object {

        private val TAG = UsbFileHttpServer::class.java.simpleName
    }
}
