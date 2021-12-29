@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Switch
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The options for setting a dialog as a list.
 * @author Arnau Mora
 * @since 20211229
 * @param items The items to display in the dialog.
 * @param dismissOnSelect If the dialog should be dismissed when selecting an option.
 */
data class ListDialogOptions(
    val items: Map<String, String>,
    val dismissOnSelect: Boolean = true
)

/**
 * The data for displaying a dialog when clicked the item.
 * @author Arnau Mora
 * @since 20211229
 * @param title The title of the dialog.
 * @param positiveButton The text of the positive button of the dialog, if null won't be displayed.
 * @param negativeButton The text of the negative button of the dialog, if null won't be displayed.
 * @param saveOnDismiss If the preference value should be stored when dismissing the dialog.
 */
data class SettingsDataDialog(
    val title: String,
    val positiveButton: String? = null,
    val negativeButton: String? = null,
    val saveOnDismiss: Boolean = true,
    val integer: Boolean = false,
    val float: Boolean = false,
    val list: ListDialogOptions? = null
)

@Composable
fun SettingsCategory(
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
@ExperimentalMaterialApi
fun SettingsItem(
    title: String,
    enabled: Boolean = true,
    subtitle: String? = null,
    icon: ImageVector? = null,
    stateBoolean: Boolean? = null,
    stateInt: Int? = null,
    stateFloat: Float? = null,
    stateString: String? = null,
    setBoolean: ((value: Boolean) -> Unit)? = null,
    setInt: ((value: Int) -> Unit)? = null,
    setFloat: ((value: Float) -> Unit)? = null,
    setString: ((value: String) -> Unit)? = null,
    checkBox: Boolean = false,
    switch: Boolean = false,
    dialog: SettingsDataDialog? = null,
    onClick: (() -> Unit)? = null
) {
    var openDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(
                enabled = (onClick != null || dialog != null) && enabled,
                onClick = {
                    onClick?.let { it() }
                    dialog?.let { openDialog = true }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
        ) {
            if (icon != null)
                Image(
                    icon,
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp)
                )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .alpha(if (enabled) 1f else ContentAlpha.disabled),
                style = MaterialTheme.typography.labelLarge
            )
            if (subtitle != null)
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .alpha(if (enabled) 1f else ContentAlpha.disabled),
                    style = MaterialTheme.typography.labelMedium
                )
        }
        if ((checkBox || switch) && stateBoolean != null)
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically)
            ) {
                if (checkBox)
                    Checkbox(
                        checked = stateBoolean,
                        enabled = enabled,
                        onCheckedChange = { value -> setBoolean?.let { it(value) } }
                    )
                else if (switch)
                    Switch(
                        checked = stateBoolean,
                        enabled = enabled,
                        onCheckedChange = { value -> setBoolean?.let { it(value) } }
                    )
            }
    }

    if (openDialog && dialog != null && (stateInt != null || stateFloat != null || dialog.list != null)) {
        var textFieldValue by remember {
            mutableStateOf(
                when {
                    dialog.integer && stateInt != null -> stateInt.toString()
                    dialog.float && stateFloat != null -> stateFloat.toString()
                    else -> ""
                }
            )
        }

        fun save() {
            if (dialog.integer)
                textFieldValue.toIntOrNull()
                    ?.let { value -> setInt?.let { it(value) } }
            else if (dialog.float)
                textFieldValue.toFloatOrNull()
                    ?.let { value -> setFloat?.let { it(value) } }
            openDialog = false
        }

        AlertDialog(
            onDismissRequest = {
                if (dialog.saveOnDismiss)
                    save()
                else
                    openDialog = false
            },
            title = { Text(text = dialog.title) },
            text = {
                if (dialog.integer || dialog.float)
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it.let {
                                var str = it
                                if (dialog.integer)
                                    str = str
                                        .replace(".", "")
                                        .replace(",", "")
                                        .replace("-", "")
                                        .replace(" ", "")
                                str
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (dialog.integer || dialog.float)
                                KeyboardType.Number
                            else KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            focusedIndicatorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                else if (dialog.list != null)
                    dialog.list.apply {
                        Column {
                            items.forEach { item ->
                                ListItem(
                                    icon = {
                                        Icon(
                                            if (stateString == item.key)
                                                Icons.Default.RadioButtonChecked
                                            else
                                                Icons.Default.RadioButtonUnchecked,
                                            contentDescription = item.value
                                        )
                                    },
                                    modifier = Modifier
                                        .clickable {
                                            setString?.let { it(item.key) }

                                            if (dismissOnSelect)
                                                openDialog = false
                                        }
                                ) {
                                    Text(text = item.value)
                                }
                            }
                        }
                    }
            },
            confirmButton = {
                if (dialog.positiveButton != null)
                    Button(
                        colors = ButtonDefaults.textButtonColors(),
                        onClick = {
                            save()
                        }
                    ) {
                        Text(text = dialog.positiveButton)
                    }
            },
            dismissButton = {
                if (dialog.negativeButton != null)
                    Button(
                        colors = ButtonDefaults.textButtonColors(),
                        onClick = {
                            openDialog = false
                        }
                    ) {
                        Text(text = dialog.negativeButton)
                    }
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(name = "Settings category")
@Composable
fun SettingsCategoryPreview() {
    SettingsCategory(text = "Settings category")
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(name = "Preview no pref")
@Composable
fun SettingsItemPreview() {
    SettingsItem(
        "Preference title",
        subtitle = "This is subtitle",
        icon = Icons.Default.Star
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(name = "Preview checkbox")
@Composable
fun SettingsItemPreviewCheckbox() {
    val stateBoolean by remember { mutableStateOf(false) }
    SettingsItem(
        "Preference title",
        subtitle = "This is subtitle",
        icon = Icons.Default.Star,
        stateBoolean = stateBoolean,
        checkBox = true
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(name = "Preview switch")
@Composable
fun SettingsItemPreviewSwitch() {
    val stateBoolean by remember { mutableStateOf(false) }
    SettingsItem(
        "Preference title",
        subtitle = "This is subtitle",
        icon = Icons.Default.Star,
        stateBoolean = stateBoolean,
        switch = true
    )
}
