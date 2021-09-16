package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import kotlinx.parcelize.Parcelize

/**
 * Stores the data for the required safes of a [Path].
 * @author Arnau Mora
 * @since 20210916
 * @param lanyardRequired Sets if it's required to bring lanyards to climb the [Path].
 * @param crackerRequired Sets if it's required to bring crackers to climb the [Path].
 * @param friendRequired Sets if it's required to bring friends to climb the [Path].
 * @param stripsRequired Sets if it's required to bring strips to climb the [Path].
 * @param pitonRequired Sets if it's required to bring pitons to climb the [Path].
 * @param nailRequired Sets if it's required to bring nails to climb the [Path].
 */
@Parcelize
data class RequiredSafesData(
    val lanyardRequired: Boolean,
    val crackerRequired: Boolean,
    val friendRequired: Boolean,
    val stripsRequired: Boolean,
    val pitonRequired: Boolean,
    val nailRequired: Boolean
) : SafesData() {
    /**
     * Converts the [RequiredSafesData] to a JSON-formatted [String].
     * @author Arnau Mora
     * @since 20210916
     */
    override fun toJSONString(): String {
        return "{" +
                "\"lanyard_required\":\"$lanyardRequired\"," +
                "\"cracker_required\":\"$crackerRequired\"," +
                "\"friend_required\":\"$friendRequired\"," +
                "\"strips_required\":\"$stripsRequired\"," +
                "\"piton_required\":\"$pitonRequired\"," +
                "\"nail_required\":\"$nailRequired\"" +
                "}"
    }

    /**
     * Returns the class as a [SafeCountData] which can be used for displaying in the UI easily.
     * @author Arnau Mora
     * @since 20210916
     */
    override fun list(): List<SafeCountData> =
        listOf(
            SafeCountData(
                lanyardRequired,
                R.string.safe_lanyard,
                R.drawable.ic_lanyard
            ),
            SafeCountData(
                crackerRequired,
                R.string.safe_cracker,
                R.drawable.ic_cracker
            ),
            SafeCountData(
                friendRequired,
                R.string.safe_friend,
                R.drawable.ic_friend
            ),
            SafeCountData(
                stripsRequired,
                R.string.safe_strips,
                R.drawable.ic_strips
            ),
            SafeCountData(
                pitonRequired,
                R.string.safe_piton,
                R.drawable.ic_buril
            ),
            SafeCountData(
                nailRequired,
                R.string.safe_nail,
                R.drawable.ic_ungla
            )
        )

    /**
     * Checks if any of the objects is required to climb the [Path].
     * @author Arnau Mora
     * @since 20210916
     * @return True if any of the attributes of the class is true.
     */
    fun any(): Boolean = lanyardRequired || crackerRequired || friendRequired || stripsRequired ||
            pitonRequired || nailRequired
}
