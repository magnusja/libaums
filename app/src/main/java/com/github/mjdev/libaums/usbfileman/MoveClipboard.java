/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
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

package com.github.mjdev.libaums.usbfileman;

import com.github.mjdev.libaums.fs.UsbFile;

/**
 * Small helper class to move files or directories to another directory. It
 * saves the instance to the {@link UsbFile} which shall be moved to another
 * place.
 * 
 * @author mjahnen
 * 
 */
public class MoveClipboard {

	private static MoveClipboard instance;
	private UsbFile file;

	private MoveClipboard() {

	}

	/**
	 * 
	 * @return The global used instance.
	 */
	public static synchronized MoveClipboard getInstance() {
		if (instance == null)
			instance = new MoveClipboard();

		return instance;
	}

	/**
	 * 
	 * @return The file saved in the clipboard.
	 */
	public synchronized UsbFile getFile() {
		return file;
	}

	/**
	 * Sets the file in the clipboard.
	 * 
	 * @param file
	 *            The file which shall be moved.
	 */
	public synchronized void setFile(UsbFile file) {
		this.file = file;
	}

}
