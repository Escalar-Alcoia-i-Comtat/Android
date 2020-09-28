package com.arnyminerz.escalaralcoiaicomtat.storage

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class UnzipUtil(private val zipFile: File, private val targetLocation: File) {
    companion object {
        private const val BUFFER_SIZE = 4096
    }

    fun unzip() {
        /*try {
            val fin = FileInputStream(zipFile)
            val zin = ZipInputStream(fin as InputStream?)
            var ze: ZipEntry?
            while (zin.nextEntry.also { ze = it } != null) {
                if (!targetLocation.exists())
                    targetLocation.mkdirs()

                val targetFile = File(targetLocation, ze!!.name)
                val targetParent = targetFile.parentFile
                if(targetParent != null && !targetParent.exists())
                    if(!targetParent.mkdirs())
                        Timber.e("Could not create parent dir: %s", targetParent)

                Timber.v("Unzipping %s", ze!!.name)

                val fout = FileOutputStream(targetFile)
                val buffer = ByteArray(8192)
                var len: Int
                while (zin.read(buffer).also { len = it } != -1)
                    fout.write(buffer, 0, len)

                fout.close()
                zin.closeEntry()
            }
            zin.close()
        } catch (e: Exception) {
            Timber.e(e, "unzip")
        }*/
        if (!targetLocation.exists()) {
            targetLocation.mkdir()
        }
        val zipIn = ZipInputStream(FileInputStream(zipFile))
        var entry = zipIn.nextEntry
        // iterates over entries in the zip file
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