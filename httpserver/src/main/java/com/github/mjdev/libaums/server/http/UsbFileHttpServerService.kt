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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.server.http.server.HttpServer
import java.io.IOException

/**
 * Service to run a [UsbFileHttpServer] in background as an Android service.
 * This class has most likely to be subclassed to add additional functionality which is dependent
 * on each individual application.
 */
class UsbFileHttpServerService : Service() {

    var server: UsbFileHttpServer? = null

    val isServerRunning: Boolean
        get() = server?.isAlive ?: false

    inner class ServiceBinder : Binder() {
        val service: UsbFileHttpServerService
            get() = this@UsbFileHttpServerService
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun startServer(file: UsbFile, server: HttpServer, notificationChannelId: String = "com.github.magnusja.libaums.http_service_channel", notificationName: CharSequence = "libaums_http") {
        startAsForeground(notificationChannelId, notificationName)

        this.server = UsbFileHttpServer(file, server)
        this.server?.start()
    }

    fun stopServer() {
        try {
            server?.stop()
        } catch (e: IOException) {
            Log.e(TAG, "exception stopping server", e)
        }

        stopForeground()
    }

    override fun onBind(intent: Intent): IBinder? {
        return ServiceBinder()
    }

    protected fun startAsForeground(notificationId: String, notificationName: CharSequence) {

        var channelId = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(notificationId, notificationName)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
                .setCategory("service")
                .setContentTitle("Serving via HTTP")
                .build()
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(id: String, name: CharSequence): String {
        val chan = NotificationChannel(id,
                name, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)

        return id
    }

    protected fun stopForeground() {
        stopForeground(true)
    }

    companion object {

        private val TAG = UsbFileHttpServerService::class.java.simpleName

        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
