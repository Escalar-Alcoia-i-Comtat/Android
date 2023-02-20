package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnyminerz.markdowntext.MarkdownFlavour
import com.arnyminerz.markdowntext.MarkdownText

@Composable
fun CardWithIcon(
    icon: ImageVector?,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = title,
    colors: CardColors = CardDefaults.cardColors(),
) {
    Card(modifier, colors = colors) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            icon?.let { Icon(it, contentDescription, modifier = Modifier.size(32.dp)) }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                content()
            }
        }
    }
}

@Composable
fun CardWithIconAndText(
    icon: ImageVector?,
    title: String,
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    contentDescription: String = title,
    colors: CardColors = CardDefaults.cardColors(),
) = CardWithIcon(icon, title, { Text(text, Modifier.fillMaxWidth()) }, modifier, contentDescription, colors)

@Composable
fun CardWithIconAndMarkdown(
    icon: ImageVector?,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String = title,
    colors: CardColors = CardDefaults.cardColors(),
) = CardWithIcon(icon, title, { MarkdownText(text, Modifier.fillMaxWidth(), flavour = MarkdownFlavour.Github) }, modifier, contentDescription, colors)

@Preview
@Composable
fun CardWithIconAndTextPreview() {
    CardWithIconAndText(
        icon = Icons.Outlined.Construction,
        contentDescription = "Testing",
        title = "Testing title",
        text = buildAnnotatedString {
            append("This is the title for a testing card. Contents can be ")
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append("styled")
            }
            append(" with AnnotatedString.")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    )
}
