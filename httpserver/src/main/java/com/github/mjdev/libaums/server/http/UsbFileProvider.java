package com.github.mjdev.libaums.server.http;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;

public interface UsbFileProvider {
    UsbFile determineFileToServe(String uri) throws IOException;
}
