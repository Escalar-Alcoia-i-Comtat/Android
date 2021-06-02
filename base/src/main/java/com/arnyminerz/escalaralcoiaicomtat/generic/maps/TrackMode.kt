package com.arnyminerz.escalaralcoiaicomtat.generic.maps

enum class TrackMode {
    /**
     * The user's position won't be followed in the screen.
     * @author Arnau Mora
     * @since 20210602
     */
    NONE,

    /**
     * The movement of the user will be animated with a camera move.
     * @author Arnau Mora
     * @since 20210602
     */
    ANIMATED,

    /**
     * The movement of the user will be followed with a camera move.
     * @author Arnau Mora
     * @since 20210602
     */
    FIXED
}
