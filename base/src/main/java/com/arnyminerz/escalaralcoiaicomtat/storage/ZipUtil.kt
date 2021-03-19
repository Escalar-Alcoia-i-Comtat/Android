package com.arnyminerz.escalaralcoiaicomtat.storage

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream

private const val BUFFER = 2048

/**
 * Compresses a file into a zip file
 * @author Arnau Mora
 * @since 20210318
 * @param file The source file
 * @param targetStream The target file's output stream
 *
 * @throws NullPointerException If the entry name is null
 * @throws IllegalArgumentException If the entry name is longer than 0xFFFF byte
 * @throws SecurityException If a security manager exists and its SecurityManager.checkRead(String)
 * method denies read access to the file
 * @throws ZipException If a ZIP format error has occurred
 * @throws IOException If an I/O error has occurre
 */
@Throws(
    NullPointerException::class,
    IllegalArgumentException::class,
    SecurityException::class,
    ZipException::class,
    IOException::class
)
fun zipFile(file: File, targetStream: OutputStream, includeSelf: Boolean) {
    val origin: BufferedInputStream
    var zipOutput: ZipOutputStream? = null
    try {
        zipOutput = ZipOutputStream(targetStream.buffered(BUFFER))
        if (file.isDirectory)
            zipSubFolder(
                zipOutput,
                file,
                if (includeSelf) file.parent!!.length else file.path.length + 1
            )
        else {
            val data = ByteArray(BUFFER)
            val fi = file.inputStream()
            origin = fi.buffered(BUFFER)
            val entry = ZipEntry(if (includeSelf) getLastPathComponent(file.path) else "")
            entry.time = file.lastModified()
            zipOutput.putNextEntry(entry)
            var count = origin.read(data, 0, BUFFER)
            while (count != -1) {
                zipOutput.write(data, 0, count)
                count = origin.read(data, 0, BUFFER)
            }
        }
    } finally {
        zipOutput?.close()
    }
}

private fun zipSubFolder(zipOutput: ZipOutputStream, folder: File, basePathLength: Int) {
    val files = folder.listFiles() ?: return
    var origin: BufferedInputStream? = null
    try {
        for (file in files) {
            if (file.isDirectory)
                zipSubFolder(zipOutput, file, basePathLength)
            else {
                val data = ByteArray(BUFFER)
                val unmodifiedFilePath = file.path
                val fi = FileInputStream(unmodifiedFilePath)
                origin = fi.buffered(BUFFER)
                val relativePath = unmodifiedFilePath.substring(basePathLength)
                val entry = ZipEntry(relativePath)
                entry.time = file.lastModified()
                zipOutput.putNextEntry(entry)
                var count = origin.read(data, 0, BUFFER)
                while (count != -1) {
                    zipOutput.write(data, 0, count)
                    count = origin.read(data, 0, BUFFER)
                }
            }
        }
    } finally {
        origin?.close()
    }
}

/*
 * gets the last path component
 *
 * Example: getLastPathComponent("downloads/example/fileToZip");
 * Result: "fileToZip"
 */
private fun getLastPathComponent(filePath: String): String {
    val segments = filePath.split("/").toTypedArray()
    return if (segments.isEmpty()) "" else segments[segments.size - 1]
}
