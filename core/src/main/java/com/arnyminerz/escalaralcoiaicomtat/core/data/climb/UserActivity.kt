package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import java.util.Date

/**
 * Stores the activity the user has performed in a session.
 * @author Arnau Mora
 * @since 20210821
 * @param zone The [Zone] where the user has been training.
 * @param completedData A list of [MarkedDataInt] which contain the info on the completions.
 */
data class UserActivity(val zone: Zone, val completedData: List<MarkedDataInt>) {
    /**
     * Gets the date when the user trained.
     * @author Arnau Mora
     * @since 20210821
     */
    val date: Date?
        get() =
            if (completedData.isEmpty())
                null
            else
            // TODO: First element may not be the oldest one, completedData should be ordered.
                completedData[0].timestamp?.toDate()
}
