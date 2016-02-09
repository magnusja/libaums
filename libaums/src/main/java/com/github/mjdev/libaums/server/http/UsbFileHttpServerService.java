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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;

/**
 * Service to run a {@link UsbFileHttpServer} in background as an Android service.
 * This class has most likely to be subclassed to add additional functionality which is dependent
 * on each individual application.
 */
public class UsbFileHttpServerService extends Service {

    public class ServiceBinder extends Binder {
        public UsbFileHttpServerService getService() {
            return UsbFileHttpServerService.this;
        }
    }

    private static final int ONGOING_NOTIFICATION_ID = 1;

    protected UsbFileHttpServer server;

    public void startServer(UsbFile file) throws IOException {
        startAsForeground();

        server = new UsbFileHttpServer(file);
        server.start();
    }

    public void stopServer() {
        server.stop();

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

    protected void startAsForeground() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Serving via HTTP")
                .build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    protected void stopForeground() {
        stopForeground(true);
    }
}
