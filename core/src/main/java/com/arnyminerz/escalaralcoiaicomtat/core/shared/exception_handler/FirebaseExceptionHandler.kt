package com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.google.firebase.storage.StorageException

/**
 * Runs checks and gets a message for the user from the exception.
 * @author Arnau Mora
 * @since 20210422
 * @param e The exception to handle
 * @return A [Pair] whose first param is a String res to show a toast, and the second one is for
 * logging purposes.
 */
fun handleStorageException(e: StorageException): Pair<Int, String>? =
    when(e.errorCode) {
        StorageException.ERROR_BUCKET_NOT_FOUND ->
            R.string.toast_error_storage_not_found to "Could not get from Storage, could not find bucket."
        StorageException.ERROR_CANCELED ->
            R.string.toast_error_storage_cancelled to "Could not get from Storage, request cancelled."
        StorageException.ERROR_INVALID_CHECKSUM ->
            R.string.toast_error_storage_corrupt to "Could not get from Storage, checksums do not match."
        StorageException.ERROR_NOT_AUTHENTICATED ->
            R.string.toast_error_storage_authentication to "Could not get from Storage, not authenticated."
        StorageException.ERROR_NOT_AUTHORIZED ->
            R.string.toast_error_storage_authentication to "Could not get from Storage, not authorised."
        StorageException.ERROR_OBJECT_NOT_FOUND ->
            R.string.toast_error_storage_not_found to "Could not get from Storage, could not find file."
        StorageException.ERROR_PROJECT_NOT_FOUND ->
            R.string.toast_error_storage_not_found to "Could not get from Storage, could not find project."
        StorageException.ERROR_QUOTA_EXCEEDED ->
            R.string.toast_error_storage_authentication to "Could not get from Storage, quota exceed."
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
            R.string.toast_error_internal to "Could not get from Storage, retry limit reached"
        StorageException.ERROR_UNKNOWN ->
            R.string.toast_error_internal to "Could not get from Storage. Http code: ${e.httpResultCode}"
        else -> null
    }
