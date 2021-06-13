package com.arnyminerz.escalaralcoiaicomtat.core.utils.storage

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

private const val BUFFER_SIZE = 4096

class UnzipUtil(private val zipFile: File, private val targetLocation: File) {
    fun unzip() {
        if (!targetLocation.exists()) {
            targetLocation.mkdir()
        }
        val zipIn = ZipInputStream(FileInputStream(zipFile))
        var entry = zipIn.nextEntry
        // iterates over entries in the zip file
        while (entry != null) {
            val file = File(targetLocation, entry.name)
            if (entry.isDirectory)
                // if the entry is a file, extracts it
                file.mkdir()
            else
                extractFile(zipIn, file)

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
    }

    private fun extractFile(zipIn: ZipInputStream, file: File) {
        val bos = BufferedOutputStream(FileOutputStream(file))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (zipIn.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }
}
