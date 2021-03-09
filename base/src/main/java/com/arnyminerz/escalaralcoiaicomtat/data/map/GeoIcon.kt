package com.arnyminerz.escalaralcoiaicomtat.data.map

import androidx.annotation.DrawableRes
import java.io.Serializable

open class GeoIcon(open val name: String) : Serializable

data class GeoIconDrawable(override val name: String, @DrawableRes val icon: Int) : GeoIcon(name)
