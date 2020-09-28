package com.arnyminerz.escalaralcoiaicomtat.data.climb.enum

enum class BlockingType(val value: Int, val idName: String) {
    UNKNOWN(-1, "NULL"),
    DRY(0, "dry"),
    BUILD(1, "build"),
    BIRD(2, "bird"),
    OLD(3, "old"),
    ROCKS(4, "rocks"),
    PLANTS(5, "plants"),
    ROPE_LENGTH(6, "rope_length");

    companion object {
        fun find(idName: String?): BlockingType {
            if (idName != null)
                for (type in values())
                    if (type.idName.startsWith(idName))
                        return type
            return UNKNOWN
        }
    }

    override fun toString(): String = idName
}