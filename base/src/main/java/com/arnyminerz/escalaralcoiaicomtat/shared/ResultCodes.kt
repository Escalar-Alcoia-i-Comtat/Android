package com.arnyminerz.escalaralcoiaicomtat.shared

/**
 * When the user has just been registered and the confirmation email has been sent.
 * @author Arnau Mora
 * @since 20210425
 */
const val RESULT_CODE_WAITING_EMAIL_CONFIRMATION = 1

/**
 * When the user has successfully been logged in.
 * @author Arnau Mora
 * @since 20210425
 */
const val RESULT_CODE_LOGGED_IN = 2

/**
 * When it's required to be logged in, but an Activity was launched without it.
 * @author Arnau Mora
 * @since 20210425
 */
const val RESULT_CODE_NOT_LOGGED_IN = 3

/**
 * When some data was required to launch the Activity, but it was not found in the Intent.
 * @author Arnau Mora
 * @since 20210425
 */
const val RESULT_CODE_MISSING_DATA = 4

/**
 * When a path has been marked as complete, this will be returned, and the Intent data will contain
 * [EXTRA_PATH].
 * @author Arnau Mora
 * @since 20210425
 * @see EXTRA_PATH
 */
const val RESULT_CODE_MARKED_AS_COMPLETE = 5

/**
 * When a path has been marked as project, this will be returned, and the Intent data will contain
 * [EXTRA_PATH].
 * @author Arnau Mora
 * @since 20210425
 * @see EXTRA_PATH
 */
const val RESULT_CODE_MARKED_AS_PROJECT = 6
