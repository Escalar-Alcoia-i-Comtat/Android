package com.arnyminerz.escalaralcoiaicomtat.activity.isolated

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isEmail
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast

/**
 * An activity that allows the user to send feedback directly to the developers.
 * @author Arnau Mora
 * @since 20220126
 */
class FeedbackActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                FeedbackWindow()
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun FeedbackWindow() {
    val context = LocalContext.current

    var fieldsEnabled by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var emailEmptyError by remember { mutableStateOf(false) }
    var emailInvalidError by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf(false) }

    val anonymousName = stringResource(R.string.feedback_anonymouse)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    if (fieldsEnabled)
                        Text(text = stringResource(R.string.action_send))
                },
                icon = {
                    Icon(
                        if (fieldsEnabled)
                            Icons.Rounded.Send
                        else
                            Icons.Rounded.Circle,
                        contentDescription = stringResource(R.string.image_desc_send_feedback_fab)
                    )
                },
                modifier = Modifier,
                onClick = {
                    fieldsEnabled = false

                    if (name.isEmpty())
                        name = anonymousName

                    if (email.isEmpty())
                        emailEmptyError = true
                    else if (!email.isEmail())
                        emailInvalidError = true
                    else if (message.isEmpty())
                        messageError = true
                    else {
                        // TODO; Email sending
                        toast(context, "Feedback currently not working.")
                    }

                    fieldsEnabled = emailEmptyError || emailInvalidError || messageError
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val emailFocusRequester = remember { FocusRequester() }
            val messageFocusRequester = remember { FocusRequester() }
            var nameFieldFocused by remember { mutableStateOf(false) }
            var emailFieldFocused by remember { mutableStateOf(false) }

            Text(
                text = stringResource(R.string.feedback_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                enabled = fieldsEnabled,
                label = {
                    Text(text = stringResource(R.string.feedback_name))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { nameFieldFocused = it.isFocused }
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        emailFocusRequester.requestFocus()
                    }
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colorScheme.onBackground
                )
            )
            AnimatedVisibility(
                visible = nameFieldFocused,
            ) {
                Text(
                    text = stringResource(R.string.feedback_name_help),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                enabled = fieldsEnabled,
                label = {
                    Text(text = stringResource(R.string.feedback_email))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .onFocusChanged {
                        emailFieldFocused = it.isFocused
                        emailEmptyError = false
                        emailInvalidError = false
                    }
                    .focusRequester(emailFocusRequester),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        messageFocusRequester.requestFocus()
                    }
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colorScheme.onBackground
                )
            )
            AnimatedVisibility(
                visible = emailFieldFocused,
            ) {
                Text(
                    text = stringResource(R.string.feedback_name_help),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                )
            }
            AnimatedVisibility(
                visible = emailEmptyError,
            ) {
                Text(
                    text = stringResource(R.string.feedback_error_empty_email),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                )
            }
            AnimatedVisibility(
                visible = emailInvalidError,
            ) {
                Text(
                    text = stringResource(R.string.feedback_error_invalid_email),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                )
            }

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                enabled = fieldsEnabled,
                label = {
                    Text(text = stringResource(R.string.feedback_message))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .onFocusChanged { messageError = false }
                    .focusRequester(messageFocusRequester),
                shape = RoundedCornerShape(8.dp),
                singleLine = false,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colorScheme.onBackground
                ),
                isError = messageError,
            )
            AnimatedVisibility(visible = messageError) {
                Text(
                    text = stringResource(R.string.feedback_error_empty_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        FeedbackWindow()
    }
}