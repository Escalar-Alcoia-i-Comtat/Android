package com.arnyminerz.escalaralcoiaicomtat.data.climb.enum

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arnyminerz.escalaralcoiaicomtat.R

enum class EndingType(val value: Int, val idName: String, @StringRes val displayName: Int) {
    Unknown(-1, "NULL", R.string.path_ending_unknown),
    Plate(0, "plate", R.string.path_ending_plate),
    PlateRing(1, "plate_ring", R.string.path_ending_plate_ring),
    PlateLanyard(2, "plate_lanyard", R.string.path_ending_plate_lanyard),
    ChainRing(3, "chain_ring", R.string.path_ending_chain_ring),
    ChainCarabiner(4, "chain_carabiner", R.string.path_ending_chain_carabiner),
    Piton(6, "piton", R.string.path_ending_piton),
    Walking(7, "walking", R.string.path_ending_walking),
    Rappel(8, "rappel", R.string.path_ending_rappel),
    Lanyard(9, "lanyard", R.string.path_ending_lanyard),
    None(5, "none", R.string.path_ending_none);

    override fun toString(): String = idName

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
                if(idName.startsWith("L"))
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

        fun fromDB(obj: String): ArrayList<EndingType>{
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

fun String?.toEnding(): EndingType =
    EndingType.find(this)