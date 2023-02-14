package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Stores the data for the installed safes in a [Path].
 * @author Arnau Mora
 * @since 20210916
 * @param quickdrawCount The amount of strings that are required in the [Path]. This is usually the
 * sum of the rest of the attributes.
 * @param paraboltCount The amount of safes that are parabolts on the [Path].
 * @param spitCount The amount of safes that are spits on the [Path].
 * @param tensorCount The amount of safes that are tensors on the [Path].
 * @param pitonCount The amount of safes that are pitons on the [Path].
 * @param burilCount The amount of safes that are burils on the [Path].
 */
@Parcelize
data class FixedSafesData(
    val quickdrawCount: Long,
    val paraboltCount: Long,
    val spitCount: Long,
    val tensorCount: Long,
    val pitonCount: Long,
    val burilCount: Long
) : SafesData() {
    @IgnoredOnParcel
    override val color: Int = R.color.dialog_green

    override fun list(): List<SafeCountData> =
        listOf(
            SafeCountData(
                paraboltCount,
                R.plurals.safe_parabolt,
                R.string.safe_parabolt_plural,
                R.drawable.ic_parabolt,
                R.string.placeholder_safe_description,
            ),
            SafeCountData(
                spitCount,
                R.plurals.safe_spit,
                R.string.safe_spits_plural,
                R.drawable.ic_spit,
                R.string.placeholder_safe_description,
            ),
            SafeCountData(
                tensorCount,
                R.plurals.safe_tensor,
                R.string.safe_tensors_plural,
                R.drawable.ic_tensor,
                R.string.placeholder_safe_description,
            ),
            SafeCountData(
                pitonCount,
                R.plurals.safe_piton,
                R.string.safe_pitons_plural,
                R.drawable.ic_reunio_clau,
                R.string.placeholder_safe_description,
            ),
            SafeCountData(
                burilCount,
                R.plurals.safe_buril,
                R.string.safe_burils_plural,
                R.drawable.ic_buril,
                R.string.placeholder_safe_description,
            )
        )

    override fun toJSONString(): String {
        return "{" +
                "\"string_count\":\"$quickdrawCount\"," +
                "\"parabolt_count\":\"$paraboltCount\"," +
                "\"spit_count\":\"$spitCount\"," +
                "\"tensor_count\":\"$tensorCount\"," +
                "\"piton_count\":\"$pitonCount\"," +
                "\"buril_count\":\"$burilCount\"" +
                "}"
    }
}
