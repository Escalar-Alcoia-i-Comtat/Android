package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.utils.DataExtra

@Deprecated("Jetpack navigation should be used")
val EXTRA_AREA = DataExtra<String>("area")

@Deprecated("Jetpack navigation should be used")
val EXTRA_ZONE = DataExtra<String>("zone")

@Deprecated("Jetpack navigation should be used")
val EXTRA_SECTOR = DataExtra<String>("sector")

@Deprecated("Jetpack navigation should be used")
val EXTRA_PATH = DataExtra<String>("path")

/**
 * Used in DataClass activities for knowing which is the DataClass to load.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_DATACLASS = DataExtra<Parcelable>("dataclass")

/**
 * Used in DataClass activities for knowing which is the parent of the loaded DataClass. Makes the
 * application more agile, since it doesn't need to be loading the same contents again and again.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_PARENT = DataExtra<Parcelable>("parent")

/**
 * Used in DataClass activities that load multiple groups of children to know which is the currently
 * selected one.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_INDEX = DataExtra<Int>("index")

val EXTRA_PATH_DOCUMENT = DataExtra<String>("path_document")

/**
 * Used for passing some initial feedback for the feedback send activity.
 * @author Arnau Mora
 * @since 20210919
 */
val EXTRA_FEEDBACK = DataExtra<String>("feedback")

/**
 * Serves as an Activity's extra parameter for passing completed paths data.
 * @author Arnau Mora
 * @since 20210821
 */
val EXTRA_COMPLETIONS = DataExtra<ArrayList<Parcelable>>("completions")

/**
 * Used together with [EXTRA_COMPLETIONS] for telling an activity how much of the items in
 * [EXTRA_COMPLETIONS] are completions.
 * @author Arnau Mora
 * @since 20210821
 */
val EXTRA_COMPLETIONS_COUNT = DataExtra<Int>("completions")

/**
 * Used together with [EXTRA_COMPLETIONS] for telling an activity how much of the items in
 * [EXTRA_COMPLETIONS] are projects.
 * @author Arnau Mora
 * @since 20210821
 */
val EXTRA_PROJECTS_COUNT = DataExtra<Int>("projects")

val EXTRA_POSITION = DataExtra<Int>("position")

val EXTRA_ZONE_TRANSITION_NAME = DataExtra<String>("zone_transition")
val EXTRA_AREA_TRANSITION_NAME = DataExtra<String>("area_transition")
val EXTRA_SECTOR_TRANSITION_NAME = DataExtra<String?>("sector_transition")

val EXTRA_KMZ_FILE = DataExtra<String>("KMZFle")
val EXTRA_CENTER_CURRENT_LOCATION = DataExtra<Boolean>("CenterLocation")

/**
 * Used in [DynamicLinkHandler] for passing [LoadingActivity] the link that is wanted to be launched
 * once the data is loaded.
 * @author Arnau Mora
 * @since 20210521
 */
val EXTRA_LINK_PATH = DataExtra<String>("Link")

/**
 * If true, informs that the Activity wasn't launched from the "normal" navigation flow, it has been
 * launched after [DataClass.getIntent], for example.
 * @author Arnau Mora
 * @since 20210521
 */
val EXTRA_STATIC = DataExtra<Boolean>("Static")

/**
 * The User's UID for selecting which user's data to load.
 * @author Arnau Mora
 * @since 20210719
 */
val EXTRA_USER_UID = DataExtra<String>("UserUid")
