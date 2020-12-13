package com.github.mjdev.libaums.usb

import java.io.IOException

class PipeException : IOException("EPIPE, endpoint seems to be stalled")