package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R

enum class EndingType(val idName: String, @StringRes val displayName: Int) {
    Unknown("NULL", R.string.path_ending_unknown),
    Plate("plate", R.string.path_ending_plate),
    PlateRing("plate_ring", R.string.path_ending_plate_ring),
    PlateLanyard("plate_lanyard", R.string.path_ending_plate_lanyard),
    ChainRing("chain_ring", R.string.path_ending_chain_ring),
    ChainCarabiner("chain_carabiner", R.string.path_ending_chain_carabiner),
    Piton("piton", R.string.path_ending_piton),
    Walking("walking", R.string.path_ending_walking),
    Rappel("rappel", R.string.path_ending_rappel),
    Lanyard("lanyard", R.string.path_ending_lanyard),
    None("none", R.string.path_ending_none);

    override fun toString(): String = idName

    val index: Int
        get() {
            for ((t, type) in values().withIndex())
                if (type.idName.startsWith(idName))
                    return t - 1
            return -1
        }

    @DrawableRes
    fun getImage(): Int {
        return when (this) {
            Unknown -> R.drawable.transparent
            None -> R.drawable.round_close_24
            Plate -> R.drawable.ic_reunio_xapes_24
            PlateRing -> R.drawable.ic_reunio_xapesargolla
            PlateLanyard -> R.drawable.ic_reunio_pont_de_roca
            ChainRing -> R.drawable.ic_reunio_cadenaargolla
            ChainCarabiner -> R.drawable.ic_reunio_cadenamosqueto
            Piton -> R.drawable.ic_reunio_clau
            Walking -> R.drawable.ic_descens_caminant
            Rappel -> R.drawable.ic_via_rappelable
            Lanyard -> R.drawable.ic_reunio_pont_de_roca
        }
    }

    fun isUnknown(): Boolean = this == Unknown

    companion object {
        fun find(idName: String?): EndingType {
            if (idName != null) {
                var idNameParsed = idName
                if (idName.startsWith("L"))
                    idNameParsed = idNameParsed.substring(idNameParsed.indexOf(' '))
                return with(idNameParsed.replace("\n", "").replace("\r", "")) {
                    for (ending in values())
                        if (this.equals(ending.idName, true))
                            return@with ending
                    Unknown
                }
            }
            return Unknown
        }

        fun fromDB(obj: String): ArrayList<EndingType> {
            val list = arrayListOf<EndingType>()

            if (obj.contains("\n"))
                for (ln in obj
                    .replace("\r", "")
                    .split("\n"))
                    list.add(find(ln))
            else
                list.add(find(obj))

            return list
        }
    }
}
