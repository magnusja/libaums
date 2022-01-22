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
package me.jahnen.libaums.usbfileman

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import me.jahnen.libaums.fs.UsbFile
import java.io.IOException
import java.util.*

/**
 * List adapter to represent the contents of an [UsbFile] directory.
 *
 * @author mjahnen
 */
class UsbFileListAdapter(
        context: Context,
        /**
         *
         * @return the directory which is currently be shown.
         */
        val currentDir: UsbFile
) : ArrayAdapter<UsbFile>(context, R.layout.list_item) {
    /**
     * Class to compare [UsbFile]s. If the [UsbFile] is an directory
     * it is rated lower than an file, ie. directories come first when sorting.
     */
    private val comparator = Comparator<UsbFile> { lhs, rhs ->
        if (lhs.isDirectory && !rhs.isDirectory) {
            return@Comparator -1
        }
        if (rhs.isDirectory && !lhs.isDirectory) {
            1
        } else lhs.name.compareTo(rhs.name, ignoreCase = true)
    }
    private var files: List<UsbFile>
    private val inflater: LayoutInflater

    /**
     * Reads the contents of the directory and notifies that the View shall be
     * updated.
     *
     * @throws IOException
     * If reading contents of a directory fails.
     */
    @Throws(IOException::class)
    fun refresh() {
        files = Arrays.asList(*currentDir.listFiles())
        Collections.sort(files, comparator)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return files.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = files[position]

        return (
                convertView ?: inflater.inflate(R.layout.list_item, parent, false)
                ).apply {
                    (findViewById<View>(R.id.name_text_view) as TextView).text =
                            file.name

                    (findViewById<View>(R.id.type_text_view) as TextView).setText(when {
                        file.isDirectory -> R.string.directory
                        else -> R.string.file
                    })
                }
    }

    override fun getItem(position: Int): UsbFile {
        return files[position]
    }

    /**
     * Constructs a new List Adapter to show [UsbFile]s.
     *
     * @param context
     * The context.
     * @param dir
     * The directory which shall be shown.
     * @throws IOException
     * If reading fails.
     */
    init {
        files = ArrayList()
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        refresh()
    }
}