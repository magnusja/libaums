package com.github.mjdev.libaums.server.http.exception;

import java.io.IOException;

/**
 * Created by magnusja on 16/12/16.
 */

public class NotAFileException extends IOException {
    public NotAFileException() {
        super("Directory listing is not supported, please request a file.");
    }
}
