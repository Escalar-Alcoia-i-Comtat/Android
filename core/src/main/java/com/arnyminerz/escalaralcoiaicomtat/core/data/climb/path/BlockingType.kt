package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

enum class BlockingType(val idName: String) {
    UNKNOWN("NULL"),
    DRY("dry"),
    BUILD("build"),
    BIRD("bird"),
    OLD("old"),
    ROCKS("rocks"),
    PLANTS("plants"),
    ROPE_LENGTH("rope_length");

    companion object {
        fun find(idName: String?): BlockingType {
            if (idName != null)
                for (type in values())
                    if (type.idName.startsWith(idName))
                        return type
            return UNKNOWN
        }
    }

    val index: Int
        get() {
            for ((t, type) in values().withIndex())
                if (type.idName.startsWith(idName))
                    return t - 1
            return -1
        }

    override fun toString(): String = idName
}
