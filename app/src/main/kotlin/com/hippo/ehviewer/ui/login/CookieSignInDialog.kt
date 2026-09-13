package com.hippo.ehviewer.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun CookieSignInDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (memberId: String, passHash: String, igneous: String?) -> Unit,
) {
    if (!show) return

    val clipboardManager = LocalClipboardManager.current
    val rawCookieState = rememberTextFieldState()
    val memberIdState = rememberTextFieldState()
    val passHashState = rememberTextFieldState()
    val igneousState = rememberTextFieldState()
    var errorResId by remember { mutableStateOf<Int?>(null) }

    fun smartParse(text: String) {
        val parsed = CookieLoginHelper.parseCookieString(text)
        var matched = false
        parsed.memberId?.let {
            memberIdState.setTextAndPlaceCursorAtEnd(it)
            matched = true
        }
        parsed.passHash?.let {
            passHashState.setTextAndPlaceCursorAtEnd(it)
            matched = true
        }
        parsed.igneous?.let {
            igneousState.setTextAndPlaceCursorAtEnd(it)
            matched = true
        }
        if (matched) {
            errorResId = null
        }
    }

    WindowDialog(
        show = true,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.cookie_sign_in_dialog_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.cookie_sign_in_tip),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // 智能粘贴/快速填充
            TextField(
                state = rawCookieState,
                label = stringResource(R.string.cookie_paste_hint),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { smartParse(rawCookieState.text.toString()) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.cookie_smart_paste),
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        val toParse = if (rawCookieState.text.isNotEmpty()) {
                            rawCookieState.text.toString()
                        } else if (!clip.isNullOrBlank()) {
                            rawCookieState.setTextAndPlaceCursorAtEnd(clip)
                            clip
                        } else {
                            ""
                        }
                        if (toParse.isNotEmpty()) {
                            smartParse(toParse)
                        }
                    },
                )
            }

            // ipb_member_id
            TextField(
                state = memberIdState,
                label = "ipb_member_id *",
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // ipb_pass_hash
            TextField(
                state = passHashState,
                label = "ipb_pass_hash *",
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // igneous (ExHentai)
            TextField(
                state = igneousState,
                label = "igneous (ExHentai)",
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorResId != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(errorResId!!),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val parsed = CookieLoginHelper.validateAndFormat(
                            memberId = memberIdState.text.toString(),
                            passHash = passHashState.text.toString(),
                            igneous = igneousState.text.toString().ifEmpty { null },
                        )
                        if (parsed == null || !parsed.isValid) {
                            errorResId = R.string.cookie_invalid
                        } else {
                            onConfirm(
                                parsed.memberId!!,
                                parsed.passHash!!,
                                parsed.igneous,
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.sign_in))
                }
            }
        }
    }
}
