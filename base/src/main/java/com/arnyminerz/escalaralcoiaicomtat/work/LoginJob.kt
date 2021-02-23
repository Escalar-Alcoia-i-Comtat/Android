package com.arnyminerz.escalaralcoiaicomtat.work

import android.content.Context
import androidx.work.*
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

const val DATA_USERNAME = "username"
const val DATA_PASSWORD = "password"
const val DATA_ERROR = "error"
const val RESULT_DATA = "result"

class LoginJob(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Timber.d("Working!")
        val username = inputData.getString(DATA_USERNAME) ?: return Result.failure()
        val password = inputData.getString(DATA_PASSWORD) ?: return Result.failure()
        Timber.v("Logging in as $username")

        val url = URL("$EXTENDED_API_URL/login")
        var client: HttpURLConnection? = null
        val outputData = Data.Builder()
        try {
            val data = "username=$username&password=$password".toByteArray()
            Timber.d("Initializing Client...")
            client = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Content-Length", data.size.toString())
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
            }
            Timber.d("Initializing output stream...")
            val output = client.outputStream
            Timber.d("Writing data...")
            output.write(data)
            Timber.d("Flush!")
            output.flush()
            Timber.d("Closing...")
            output.close()
            Timber.d("Closed!")
            val lines = arrayListOf<String>()
            if (client.responseCode == HttpURLConnection.HTTP_OK) {
                Timber.d("Reading response...")
                val bufferedReader = client.inputStream.bufferedReader()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null)
                    line?.let {
                        lines.add(it)
                    }
            } else {
                Timber.e("Could not log in! Response code: ${client.responseCode}. Error message:")
                val bufferedReader = client.errorStream.bufferedReader()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    Timber.e(line)
                    if (line != null)
                        try {
                            val json = JSONObject(line!!)
                            val error = json.getString("error")
                            outputData.putString(DATA_ERROR, error)
                            return Result.failure(outputData.build())
                        } catch (ex: JSONException) {
                            Timber.e("Could not parse JSON response!")
                        }
                }
                return Result.failure()
            }
            outputData.putStringArray(RESULT_DATA, lines.toTypedArray())
        } catch (e: MalformedURLException) {
            Timber.e(e, "Could not send login request:")
            client?.disconnect()
            return Result.failure()
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Could not send login request:")
            client?.disconnect()
            return Result.failure()
        } catch (e: IOException) {
            Timber.e(e, "Could not send login request:")
            client?.disconnect()
            return Result.failure()
        }
        client.disconnect()

        return Result.success(outputData.build())
    }
}