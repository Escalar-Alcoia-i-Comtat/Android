package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.completion

/**
 * Sets the type of completion the user made of a path.
 * @author Arnau Mora
 * @since 20210502
 * @param id The identification made of the completion type
 */
enum class CompletionType(val id: String) {
    FIRST("first"),
    TOP_ROPE("top_rope")
}
