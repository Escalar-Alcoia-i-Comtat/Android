package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import kotlinx.parcelize.IgnoredOnParcel
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
    @IgnoredOnParcel
    override val color: Int = R.color.dialog_blue

    override fun toJSONString(): String = "{" +
            "\"lanyard_required\":\"$lanyardRequired\"," +
            "\"cracker_required\":\"$crackerRequired\"," +
            "\"friend_required\":\"$friendRequired\"," +
            "\"strips_required\":\"$stripsRequired\"," +
            "\"piton_required\":\"$pitonRequired\"," +
            "\"nail_required\":\"$nailRequired\"" +
            "}"

    override fun list(): List<SafeCountData> =
        listOf(
            SafeCountData(
                lanyardRequired,
                R.plurals.safe_required_lanyard,
                R.drawable.ic_lanyard,
                R.string.safe_required_lanyard_description,
            ),
            SafeCountData(
                crackerRequired,
                R.plurals.safe_required_nut,
                R.drawable.ic_cracker,
                R.string.safe_required_nut_description,
            ),
            SafeCountData(
                friendRequired,
                R.plurals.safe_required_friend,
                R.drawable.ic_friend,
                R.string.safe_required_friend_description,
            ),
            SafeCountData(
                stripsRequired,
                R.plurals.safe_required_strips,
                R.drawable.ic_strips,
                R.string.safe_required_strips_description,
            ),
            SafeCountData(
                pitonRequired,
                R.plurals.safe_required_piton,
                R.drawable.ic_buril,
                R.string.safe_piton_description,
            ),
            SafeCountData(
                nailRequired,
                R.plurals.safe_required_hook,
                R.drawable.ic_ungla,
                R.string.safe_required_hook_description,
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
