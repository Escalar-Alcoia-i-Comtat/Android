package com.arnyminerz.escalaralcoiaicomtat.async

import java.io.Serializable

/**
 * Serves as a listener for async tasks
 */
interface ResultListener<R: Serializable> {
    /**
     * When the task has finished executing correctly
     * @param result The data the task returned. Check JavaDoc for the specific function.
     */
    fun onCompleted(result: R)

    /**
     * When an error occurred while executing the task.
     * @param error The error that occurred
     */
    fun onFailure(error: Exception?)
}

/**
 * Serves as a listener for async tasks with Progress callbacks
 */
interface ResultProgressListener<R : Serializable, P : Number> {
    /**
     * When the task has finished executing correctly
     * @param result The data the task returned. Check JavaDoc for the specific function.
     */
    fun onCompleted(result: R)

    /**
     * When the task got some progress data
     * @param progress The current progress of the task
     * @param max The maximum value that progress can get
     */
    fun onProgress(progress: P, max: P)

    /**
     * When an error occurred while executing the task.
     * @param error The error that occurred
     */
    fun onFailure(error: Exception?)
}

/**
 * Serves as a listener for async tasks
 */
interface NoResultListener {
    /**
     * When the task has finished executing correctly
     */
    fun onCompleted()

    /**
     * When an error occurred while executing the task.
     * @param error The error that occurred
     */
    fun onFailure(error: Exception?)
}

/**
 * Serves as a listener for async tasks with Progress callbacks without any response content
 */
interface NoResultProgressListener<P : Number> {
    /**
     * When the task has finished executing correctly
     */
    fun onCompleted()

    /**
     * When the task got some progress data
     * @param progress The current progress of the task
     * @param max The maximum value that progress can get
     */
    fun onProgress(progress: P, max: P)

    /**
     * When an error occurred while executing the task.
     * @param error The error that occurred
     */
    fun onFailure(error: Exception?)
}