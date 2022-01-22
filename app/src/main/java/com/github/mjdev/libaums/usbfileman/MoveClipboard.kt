/*
 * (C) Copyright 2014 mjahnen <github@mgns.tech>
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
package com.github.mjdev.libaums.usbfileman

import com.github.mjdev.libaums.fs.UsbFile

/**
 * Small helper class to move files or directories to another directory. It
 * saves the instance to the [UsbFile] which shall be moved to another
 * place.
 *
 * @author mjahnen
 */
object MoveClipboard  {
    /**
     *
     * @return The file saved in the clipboard.
     */
    /**
     * Sets the file in the clipboard.
     *
     * @param file
     * The file which shall be moved.
     */
    @get:Synchronized
    @set:Synchronized
    var file: UsbFile? = null
}