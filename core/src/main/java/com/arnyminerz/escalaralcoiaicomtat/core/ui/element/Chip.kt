package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.compose.material.ChipColors
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.utils.tint
import com.google.android.material.chip.Chip

/**
 * The chip colors to be used with Material3.
 * @author Arnau Mora
 * @since 20220226
 */
@ExperimentalMaterialApi
val Material3ChipColors: ChipColors
    @Composable
    get() = ChipDefaults.chipColors(
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        leadingIconContentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
private fun rememberChipWithLifecycle(
    text: String,
    enabled: Boolean = true,
    icon: Drawable? = null,
    @ColorInt iconTint: Int? = null,
    onClick: (() -> Unit)? = null
): Chip {
    val context = LocalContext.current
    val chip = remember {
        Chip(context).apply {
            id = R.id.chip
            this.text = text
            this.isEnabled = enabled
            if (icon != null)
                this.chipIcon = icon.tint(iconTint)
            this.setOnClickListener { onClick?.let { it() } }
        }
    }
    return chip
}

/**
 * Creates a new Chip view.
 * @author Arnau Mora (ArnyminerZ)
 * @since 20211230
 * @param text The text of the chip.
 * @param modifier Modifiers to apply to the view.
 * @param enabled Whether or not the chip should be displayed as enabled.
 * @param onClick A callback for when the chip is clicked.
 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Drawable? = null,
    @ColorInt iconTint: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val chipState = rememberChipWithLifecycle(text, enabled, icon, iconTint, onClick)
    AndroidView(factory = { chipState }, modifier, update = {
        it.text = text
        it.isEnabled = enabled
        it.chipIcon = icon?.tint(iconTint)
    })
}
