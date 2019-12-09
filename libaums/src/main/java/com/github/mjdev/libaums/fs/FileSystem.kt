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

package com.github.mjdev.libaums.fs

/**
 * This class represents a file system.
 *
 * @author mjahnen
 */
interface FileSystem {

    /**
     * This method returns the root directory of the file system.
     *
     * @return The root directory of the file system.
     */
    val rootDirectory: UsbFile

    /**
     * This method returns the name of the volume which is mostly saved in the
     * file system.
     *
     *
     * In Windows the name of a volume is shown in the explorer before the drive
     * letter.
     *
     * @return
     */
    val volumeLabel: String

    /**
     *
     * @return The total "capacity" of the file system in bytes.
     */
    val capacity: Long

    /**
     * ATTENTION: This value can be inaccurate, depending on actual file system and certain file
     * system specific properties. Use as an estimation.
     * @return Occupied space in the file system in bytes (estimate).
     */
    val occupiedSpace: Long

    /**
     * ATTENTION: This value can be inaccurate, depending on actual file system and certain file
     * system specific properties. Use as an estimation.
     * @return Free space in the file system in bytes (estimate).
     */
    val freeSpace: Long

    /**
     *
     * @return returns the optimal chuck size in bytes (you should read and write in multiples of that)
     */
    val chunkSize: Int

    /**
     *
     * @return file system type [com.github.mjdev.libaums.partition.PartitionTypes]
     */
    val type: Int
}
