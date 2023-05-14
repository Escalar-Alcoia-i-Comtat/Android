package com.arnyminerz.escalaralcoiaicomtat.core

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.SafeCountData.Companion.formatCountLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeCountDataTest {
    private val stringFormat = "{0,choice,0#Quickdraws|1#{0} Quickdraw|1<{0} Quickdraws}"

    @Test
    fun formatCountLabel_test() {
        assertEquals(
            "Quickdraws",
            formatCountLabel(stringFormat, 0)
        )
        assertEquals(
            "1 Quickdraw",
            formatCountLabel(stringFormat, 1)
        )
        assertEquals(
            "2 Quickdraws",
            formatCountLabel(stringFormat, 2)
        )
    }
}
