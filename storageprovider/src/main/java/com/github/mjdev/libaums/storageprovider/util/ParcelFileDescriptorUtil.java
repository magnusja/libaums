/**
 * Copyright © unknown year Mark Murphy
 *             2014-2015 Jan Seeger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mjdev.libaums.storageprovider.util;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ParcelFileDescriptor Utility class.
 * Based on CommonsWare's ParcelFileDescriptorUtil.
 */
public class ParcelFileDescriptorUtil {
    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

        new TransferThread(inputStream, output).start();

        return pipe[0];
    }

    @SuppressWarnings("unused")
    public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);

        new TransferThread(input, outputStream).start();

        return pipe[1];
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(mIn, mOut);
                mOut.flush();
            } catch (IOException e) {
                Log.e("TransferThread", "writing failed");
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(mIn);
                IOUtils.closeQuietly(mOut);
            }
        }
    }
}