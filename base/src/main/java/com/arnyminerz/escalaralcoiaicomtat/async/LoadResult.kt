package com.arnyminerz.escalaralcoiaicomtat.async

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import java.io.Serializable

open class LoadResult<R : Serializable>(private val context: Context? = null) {
    private var resultListeners = arrayListOf<ResultListener<R>>()
    private var result: R? = null

    fun listen(resultListener: ResultListener<R>) {
        resultListeners.add(resultListener)
    }

    open fun onCompleted(result: R) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onCompleted(result)
                }
            else
                listener.onCompleted(result)
    }

    open fun onFailure(error: Exception?) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onFailure(error)
                }
            else
                listener.onFailure(error)
    }

    fun put(value: R) {
        result = value
    }

    fun get(): R? = result
}

open class LoadResultProgress<R : Serializable, P : Number>(private val context: Context? = null) {
    private var resultListeners = arrayListOf<ResultProgressListener<R, P>>()
    private var result: R? = null

    fun listen(resultListener: ResultProgressListener<R, P>) {
        resultListeners.add(resultListener)
    }

    open fun onCompleted(result: R) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onCompleted(result)
                }
            else
                listener.onCompleted(result)
    }

    open fun onProgress(progress: P, max: P) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onProgress(progress, max)
                }
            else
                listener.onProgress(progress, max)
    }

    open fun onFailure(error: Exception?) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onFailure(error)
                }
            else
                listener.onFailure(error)
    }

    fun put(value: R) {
        result = value
    }

    fun get(): R? = result
}

class LoadNoResult(private val context: Context? = null) {
    private var resultListeners = arrayListOf<NoResultListener>()

    fun listen(resultListener: NoResultListener) =
        resultListeners.add(resultListener)

    fun onCompleted() {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onCompleted()
                }
            else
                listener.onCompleted()
    }

    fun onFailure(error: Exception?) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onFailure(error)
                }
            else
                listener.onFailure(error)
    }
}

open class LoadNoResultProgress<P : Number>(private val context: Context? = null) {
    private var resultListeners = arrayListOf<NoResultProgressListener<P>>()

    fun listen(resultListener: NoResultProgressListener<P>) =
        resultListeners.add(resultListener)

    open fun onCompleted() {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onCompleted()
                }
            else
                listener.onCompleted()
    }

    open fun onProgress(progress: P, max: P) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onProgress(progress, max)
                }
            else
                listener.onProgress(progress, max)
    }

    open fun onFailure(error: Exception?) {
        for (listener in resultListeners)
            if (context != null)
                context.runOnUiThread {
                    listener.onFailure(error)
                }
            else
                listener.onFailure(error)
    }
}