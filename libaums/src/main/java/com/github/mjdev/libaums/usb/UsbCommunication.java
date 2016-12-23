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

package com.github.mjdev.libaums.usb;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This Interface describes a low level device to perform USB transfers. At the
 * moment only bulk IN and OUT transfer are supported. Every class that follows
 * {@link com.github.mjdev.libaums.driver.BlockDeviceDriver} can use this to
 * communicate with the underlying USB stack.
 * 
 * @author mjahnen
 * 
 */
public interface UsbCommunication {
	int TRANSFER_TIMEOUT = 5000;

	/**
	 * Performs a bulk out transfer beginning at the offset specified in the
	 * <code>buffer</code> of length <code>buffer#remaining()</code>.
	 * 
	 * @param src
	 *            The data to transfer.
	 * @return Bytes transmitted if successful.
	 */
	int bulkOutTransfer(ByteBuffer src) throws IOException;

	/**
	 * Performs a bulk in transfer beginning at offset zero in the
	 * <code>buffer</code> of length <code>buffer#remaining()</code>.
	 * 
	 * @param dest
	 *            The buffer where data should be transferred.
	 * @return Bytes read if successful.
	 */
	int bulkInTransfer(ByteBuffer dest) throws IOException;
}
