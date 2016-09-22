package com.github.mjdev.libaums.partition;

import java.io.IOException;

/**
 * Created by poudanen on 22.09.16.
 */

public class PartitionException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int errorCode;

    public PartitionException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
