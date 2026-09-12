package com.ehviewer.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import top.yukonga.miuix.kmp.basic.Card

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrystalCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    modifier = modifier.combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Button,
        onClick = onClick,
        onLongClick = onLongClick,
    ),
    content = content,
)

@Composable
fun CrystalCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    content = content,
)

@Composable
fun CrystalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    modifier = modifier,
    content = content,
)
