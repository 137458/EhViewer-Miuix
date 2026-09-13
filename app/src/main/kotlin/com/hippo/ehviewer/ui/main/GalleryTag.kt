package com.hippo.ehviewer.ui.main

import android.content.Context
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ehviewer.core.model.GalleryTagGroup
import com.ehviewer.core.model.PowerStatus
import com.ehviewer.core.model.TagNamespace
import com.ehviewer.core.model.VoteStatus
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.tools.includeFontPadding

@Composable
context(_: Context)
fun GalleryTags(
    tagGroups: List<GalleryTagGroup>,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String, String, VoteStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canTranslate = Settings.showTagTranslations.value && EhTagDatabase.translatable && EhTagDatabase.initialized
    val ehTags = EhTagDatabase.takeIf { canTranslate }
    fun TagNamespace.translate() = ehTags?.getTranslation(tag = value) ?: value
    fun String.translate(ns: TagNamespace) = ehTags?.getTranslation(prefix = ns.prefix, tag = this) ?: this
    val showVote by Settings.showVoteStatus.collectAsState()
    Column(modifier) {
        tagGroups.forEach { (ns, tags) ->
            Row {
                BaseRoundText(
                    text = ns.translate(),
                    isGroup = true,
                )
                FlowRow {
                    tags.forEach { (text, power, vote) ->
                        val translation = text.translate(ns)
                        val tag = ns.value + ":" + text
                        val hapticFeedback = LocalHapticFeedback.current
                        Box {
                            BaseRoundText(
                                text = translation,
                                weak = power == PowerStatus.Weak,
                                solid = power == PowerStatus.Solid && showVote,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onTagClick(tag) },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTagLongClick(tag, translation, vote)
                                    },
                                ),
                            )
                            if (vote != VoteStatus.None && showVote) {
                                Text(
                                    text = vote.display,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 2.dp),
                                    color = MiuixTheme.colorScheme.error,
                                    style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseRoundText(
    text: String,
    modifier: Modifier = Modifier,
    weak: Boolean = false,
    solid: Boolean = false,
    isGroup: Boolean = false,
) {
    val bgColor = if (isGroup) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isGroup) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(GalleryTagCorner)
            .background(bgColor),
    ) {
        Text(
            text = text,
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp).width(IntrinsicSize.Max),
            color = if (weak) MiuixTheme.colorScheme.onSurfaceVariantSummary else contentColor,
            style = MiuixTheme.textStyles.footnote1.includeFontPadding,
            textDecoration = if (solid) TextDecoration.Underline else null,
        )
    }
}

private val GalleryTagCorner = RoundedCornerShape(64.dp)
