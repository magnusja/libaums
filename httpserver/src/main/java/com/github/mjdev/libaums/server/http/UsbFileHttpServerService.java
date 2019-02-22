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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.server.http.server.HttpServer;

import java.io.IOException;

/**
 * Service to run a {@link UsbFileHttpServer} in background as an Android service.
 * This class has most likely to be subclassed to add additional functionality which is dependent
 * on each individual application.
 */
public class UsbFileHttpServerService extends Service {

    private static final String TAG = UsbFileHttpServerService.class.getSimpleName();

    public class ServiceBinder extends Binder {
        public UsbFileHttpServerService getService() {
            return UsbFileHttpServerService.this;
        }
    }

    private static final int ONGOING_NOTIFICATION_ID = 1;

    protected UsbFileHttpServer server;

    public void startServer(UsbFile file, HttpServer server) throws IOException {
        startServer(file, server, "com.github.magnusja.libaums.http_service_channel", "libaums_http");
    }

    public void startServer(UsbFile file, HttpServer server, String notificationChannelId, CharSequence notificationName) throws IOException {
        startAsForeground(notificationChannelId, notificationName);

        this.server = new UsbFileHttpServer(file, server);
        this.server.start();
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (IOException e) {
            Log.e(TAG, "exception stopping server", e);
        }

        stopForeground();
    }

    public boolean isServerRunning() {
        return server != null && server.isAlive();
    }

    public UsbFileHttpServer getServer() {
        return server;
    }

    public void setServer(UsbFileHttpServer server) {
        this.server = server;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    protected void startAsForeground(String notificationId, CharSequence notificationName) {

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(notificationId, notificationName);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setCategory("service")
                .setContentTitle("Serving via HTTP")
                .build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String id, CharSequence name) {
        NotificationChannel chan = new NotificationChannel(id,
                name, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);

        return id;
    }

    protected void stopForeground() {
        stopForeground(true);
    }
}
