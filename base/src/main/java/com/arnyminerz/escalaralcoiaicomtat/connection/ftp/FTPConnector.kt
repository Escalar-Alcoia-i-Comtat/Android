package com.arnyminerz.escalaralcoiaicomtat.connection.ftp

import android.graphics.BitmapFactory
import com.arnyminerz.escalaralcoiaicomtat.generic.cutSquare
import com.arnyminerz.escalaralcoiaicomtat.generic.scale
import com.arnyminerz.escalaralcoiaicomtat.generic.toInputStream
import org.apache.commons.net.ftp.FTP
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

class FTPClient(private val host: String, val user: String, private val password: String) :
    org.apache.commons.net.ftp.FTPClient() {
    private fun connect(): Boolean {
        try {
            connect(host)
            return if (login(user, password)) {
                enterLocalPassiveMode()
                setFileType(FTP.BINARY_FILE_TYPE)
                true
            } else false
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

    fun uploadImage(fileInputStream: FileInputStream, uploadPath: String): Boolean {
        Timber.v("Uploading image to \"$host\"...")
        Timber.v("Loading bitmap...")
        val bitmap = BitmapFactory.decodeStream(fileInputStream)
        Timber.v("Cropping bitmap...")
        val croppedBitmap = bitmap.cutSquare()
        Timber.v("Scaling bitmap...")
        val scaledBitmap = croppedBitmap.scale(512, 512)
        Timber.v("Getting stream...")
        val bitmapStream = scaledBitmap.toInputStream()

        Timber.v("Connecting...")
        if (connect()) {
            Timber.v("Connected!")
            var result = false
            try {
                result = storeFile(uploadPath, bitmapStream)
                fileInputStream.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                logout()
                disconnect()
                return result
            }
        }
        return false
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun upload(fileInputStream: FileInputStream, uploadPath: String): Boolean {
        if (connect()) {
            var result = false
            try {
                result = storeFile(uploadPath, fileInputStream)
                fileInputStream.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                logout()
                disconnect()
                return result
            }
        }
        return false
    }

    @Suppress("unused")
    fun upload(file: File, uploadPath: String): Boolean {
        return upload(file.inputStream(), uploadPath)
    }

    fun download(file: File, path: String): Boolean {
        if (connect()) {
            var result = false
            try {
                val out = file.outputStream()
                result = retrieveFile(path, out)
                out.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                logout()
                disconnect()
                return result
            }
        }
        return false
    }
}

fun ftpConnect(host: String, user: String, password: String): FTPClient {
    return FTPClient(host, user, password)
}