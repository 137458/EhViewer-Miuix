package com.hippo.ehviewer.ui.login

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.ehviewer.core.network.EhCookieStore
import com.ehviewer.core.ui.component.LiquidGlassSurface
import com.ehviewer.core.ui.component.SquircleShape
import com.ehviewer.core.ui.util.LocalWindowSizeClass
import com.ehviewer.core.ui.util.isExpanded
import com.ehviewer.core.util.launchIO
import com.ehviewer.core.util.withUIContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.destinations.WebViewSignInScreenDestination
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.awaitConfirmationOrCancel
import com.hippo.ehviewer.util.displayString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import moe.tarsin.navigate
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>(start = true)
@Composable
fun AnimatedVisibilityScope.SignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val windowSizeClass = LocalWindowSizeClass.current
    val focusManager = LocalFocusManager.current
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }
    var showUsernameError by rememberSaveable { mutableStateOf(false) }
    var showPasswordError by rememberSaveable { mutableStateOf(false) }
    var showCookieDialog by rememberSaveable { mutableStateOf(false) }
    val username = rememberTextFieldState()
    val password = rememberTextFieldState()
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    var signInJob by remember { mutableStateOf<Job?>(null) }

    CookieSignInDialog(
        show = showCookieDialog,
        onDismissRequest = { showCookieDialog = false },
        onConfirm = { memberId, passHash, igneous ->
            showCookieDialog = false
            focusManager.clearFocus()
            isProgressIndicatorVisible = true
            EhUtils.signOut()
            signInJob = launchIO {
                runCatching {
                    EhCookieStore.setIdentityCookies(memberId, passHash, igneous)
                    EhCookieStore.flush()
                    postLogin().await()
                }.onFailure {
                    withUIContext {
                        isProgressIndicatorVisible = false
                        awaitConfirmationOrCancel(
                            confirmText = R.string.get_it,
                            title = R.string.sign_in_failed,
                            showCancelButton = false,
                            text = { Text(it.displayString()) },
                        )
                    }
                }
            }
        },
    )

    fun signIn() {
        if (signInJob?.isActive == true) return
        if (username.text.isEmpty()) {
            showUsernameError = true
            return
        } else {
            showUsernameError = false
        }
        if (password.text.isEmpty()) {
            showPasswordError = true
            return
        } else {
            showPasswordError = false
        }
        focusManager.clearFocus()
        isProgressIndicatorVisible = true

        EhUtils.signOut()
        signInJob = launchIO {
            runCatching {
                EhEngine.signIn(username.text.toString(), password.text.toString())
            }.onFailure {
                withUIContext {
                    focusManager.clearFocus()
                    isProgressIndicatorVisible = false
                    awaitConfirmationOrCancel(
                        confirmText = R.string.get_it,
                        title = R.string.sign_in_failed,
                        showCancelButton = false,
                        text = {
                            Text(
                                """
                                ${it.displayString()}
                                ${stringResource(R.string.sign_in_failed_tip, stringResource(R.string.sign_in_via_webview))}
                                """.trimIndent(),
                            )
                        },
                    )
                }
            }.onSuccess {
                postLogin()
            }
        }
    }

    @Composable
    fun UsernameAndPasswordTextField() {
        TextField(
            state = username,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentType = ContentType.Username },
            label = stringResource(R.string.username),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            trailingIcon = if (showUsernameError) {
                { Icon(imageVector = MiuixIcons.Info, contentDescription = null, tint = MiuixTheme.colorScheme.error) }
            } else {
                null
            },
        )
        if (showUsernameError) {
            Text(
                text = stringResource(R.string.error_username_cannot_empty),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        TextField(
            state = password,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentType = ContentType.Password },
            label = stringResource(R.string.password),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onKeyboardAction = { signIn() },
            lineLimits = TextFieldLineLimits.SingleLine,
            outputTransformation = if (passwordHidden) OutputTransformation { replace(0, length, "\u2022".repeat(length)) } else null,
            trailingIcon = {
                if (showPasswordError) {
                    Icon(imageVector = MiuixIcons.Info, contentDescription = null, tint = MiuixTheme.colorScheme.error)
                } else {
                    IconButton(onClick = { passwordHidden = !passwordHidden }) {
                        val visibilityIcon = if (passwordHidden) MiuixIcons.Show else MiuixIcons.Hide
                        Icon(imageVector = visibilityIcon, contentDescription = null)
                    }
                }
            },
        )
        if (showPasswordError) {
            Text(
                text = stringResource(R.string.error_password_cannot_empty),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Box(contentAlignment = Alignment.Center) {
        when {
            !windowSizeClass.isExpanded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .systemBarsPadding()
                        .padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .widthIn(max = dimensionResource(id = com.hippo.ehviewer.R.dimen.single_max_width))
                            .fillMaxWidth(),
                        shape = SquircleShape(24.dp),
                        elevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(id = com.hippo.ehviewer.R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            UsernameAndPasswordTextField()
                            Text(
                                text = stringResource(id = R.string.app_warning),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Text(
                                text = stringResource(id = R.string.app_warning_2),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                style = MiuixTheme.textStyles.title4,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { openBrowser(EhUrl.URL_REGISTER) },
                                    colors = ButtonDefaults.buttonColors(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.register))
                                }
                                Button(
                                    onClick = ::signIn,
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.sign_in))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(
                                    text = stringResource(id = R.string.sign_in_via_webview),
                                    onClick = { navigate(WebViewSignInScreenDestination) },
                                )
                                TextButton(
                                    text = stringResource(id = R.string.sign_in_via_cookie),
                                    onClick = { showCookieDialog = true },
                                )
                                TextButton(
                                    text = stringResource(id = R.string.guest_mode),
                                    onClick = {
                                        Settings.gallerySite.value = EhUrl.SITE_E
                                        Settings.needSignIn.value = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .systemBarsPadding()
                        .padding(dimensionResource(id = com.hippo.ehviewer.R.dimen.keyline_margin)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .width(dimensionResource(id = com.hippo.ehviewer.R.dimen.signinscreen_landscape_caption_frame_width))
                            .padding(end = 16.dp),
                        shape = SquircleShape(24.dp),
                        elevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(id = com.hippo.ehviewer.R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.app_warning),
                                modifier = Modifier.fillMaxWidth(),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.app_warning_2),
                                modifier = Modifier.fillMaxWidth(),
                                style = MiuixTheme.textStyles.title3,
                            )
                        }
                    }
                    LiquidGlassSurface(
                        modifier = Modifier
                            .widthIn(max = dimensionResource(id = com.hippo.ehviewer.R.dimen.single_max_width))
                            .fillMaxWidth(),
                        shape = SquircleShape(24.dp),
                        elevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            UsernameAndPasswordTextField()
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = { openBrowser(EhUrl.URL_REGISTER) },
                                    colors = ButtonDefaults.buttonColors(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.register))
                                }
                                Button(
                                    onClick = ::signIn,
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(id = R.string.sign_in))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(
                                    text = stringResource(id = R.string.sign_in_via_webview),
                                    onClick = { navigate(WebViewSignInScreenDestination) },
                                )
                                TextButton(
                                    text = stringResource(id = R.string.sign_in_via_cookie),
                                    onClick = { showCookieDialog = true },
                                )
                                TextButton(
                                    text = stringResource(id = R.string.guest_mode),
                                    onClick = {
                                        Settings.gallerySite.value = EhUrl.SITE_E
                                        Settings.needSignIn.value = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (isProgressIndicatorVisible) {
            InfiniteProgressIndicator()
        }
    }
}
