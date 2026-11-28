/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.ehviewer.ui

import android.content.Context
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource
import top.yukonga.miuix.kmp.basic.Surface

context(ctx: Context)
fun isAuthenticationSupported(): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    return BiometricManager.from(ctx).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

@Composable
fun SecurityScreen(onError: () -> Unit, modifier: Modifier) {
    val security by Settings.security.collectAsState()
    // 配置变更（旋转/深色切换）后必须保持“已解锁”状态，否则每次重建都重新要求解锁
    var locked by rememberSaveable { mutableStateOf(security) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val title = stringResource(R.string.settings_privacy_require_unlock)
    val launcher = rememberAuthenticationLauncher { result ->
        isAuthenticating = false
        when (result) {
            is AuthenticationResult.Success -> locked = false
            else -> onError()
        }
    }

    @Synchronized
    fun startAuthentication() {
        if (isAuthenticating) return
        isAuthenticating = true
        val request = biometricRequest(title, Biometric.Fallback.DeviceCredential) {}
        launcher.launch(request)
    }

    val securityDelay by Settings.securityDelay.collectAsState { it.minutes }
    var leaveTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    // key 固定为 Unit，闭包会捕获首帧的 security/securityDelay，必须通过 rememberUpdatedState 读最新值
    val currentSecurity by rememberUpdatedState(security)
    val currentSecurityDelay by rememberUpdatedState(securityDelay)
    LifecycleResumeEffect(Unit) {
        if (currentSecurity && leaveTime.elapsedNow() > currentSecurityDelay) {
            locked = true
        }
        onPauseOrDispose {
            if (!locked) {
                leaveTime = TimeSource.Monotonic.markNow()
            }
        }
    }

    if (locked) {
        val windowInfo = LocalWindowInfo.current
        LaunchedEffect(windowInfo) {
            snapshotFlow { windowInfo.isWindowFocused }.collect {
                if (it) startAuthentication()
            }
        }
        Surface(
            modifier = modifier
                .fillMaxSize()
                // 遮罩必须吃掉全部指针事件，否则点击会穿透到被锁住的界面
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {}
    }
}
