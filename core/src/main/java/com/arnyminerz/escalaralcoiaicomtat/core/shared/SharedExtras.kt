package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.utils.DataExtra

/**
 * Used in DataClass activities for knowing which is the DataClass to load.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_DATACLASS = DataExtra<Parcelable>("dataclass")

/**
 * Used in DataClass activities that load multiple groups of children to know how many children
 * there are in the parent DataClass.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_CHILDREN_COUNT = DataExtra<Int>("children_count")

/**
 * Used in DataClass activities that load multiple groups of children to know which is the currently
 * selected one.
 * @author Arnau Mora
 * @since 20220106
 */
val EXTRA_INDEX = DataExtra<Int?>("index")

/**
 * Used in [DynamicLinkHandler] for passing [LoadingActivity] the link that is wanted to be launched
 * once the data is loaded.
 * @author Arnau Mora
 * @since 20210521
 */
val EXTRA_LINK_PATH = DataExtra<String>("Link")

/**
 * Used in the Warning Activity to tell the user that the preference storage has been changed, and
 * a migration will be performed.
 * @author Arnau Mora
 * @since 20220316
 */
val EXTRA_WARNING_PREFERENCE = DataExtra<Boolean>("WarningPreference")

/**
 * Used in the Warning Activity to tell the user that their device is not compatible with Google
 * Play Services and some features may not work correctly.
 * @author Arnau Mora
 * @since 20220316
 */
val EXTRA_WARNING_PLAY_SERVICES = DataExtra<Boolean>("WarningPlayServices")

/**
 * Used in the Warning Activity to tell the user that their device is not compatible with MD5
 * hashing. This may cause that updates don't work.
 * @author Arnau Mora
 * @since 20220327
 */
val EXTRA_WARNING_MD5 = DataExtra<Boolean>("WarningMd5")

val EXTRA_WARNING_INTENT = DataExtra<Parcelable>("WarningIntent")
