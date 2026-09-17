package com.hippo.ehviewer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 官方 Miuix / HyperOS 视觉规范 Markdown 杂志感富文本排版组件。
 * 提供标题左侧竖向强调重音条（Accent Indicator Bar）、圆环列表、引用块卡片、
 * 渐隐分割线、加粗与等宽代码胶囊高亮。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseFontSize: Int = 13,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        // 标题左侧主题色垂直重音条
                        if (block.level <= 2) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(if (block.level == 1) 18.dp else 15.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MiuixTheme.colorScheme.primary),
                            )
                        }
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = when (block.level) {
                                1 -> MiuixTheme.textStyles.title3.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (baseFontSize + 4).sp,
                                )
                                2 -> MiuixTheme.textStyles.title4.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (baseFontSize + 2).sp,
                                )
                                else -> MiuixTheme.textStyles.body1.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (baseFontSize + 1).sp,
                                )
                            },
                            color = if (block.level <= 2) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        )
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "${block.number}. ",
                            style = MiuixTheme.textStyles.body2.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = buildAnnotatedContent(block.text),
                            style = MiuixTheme.textStyles.body2.copy(
                                fontSize = baseFontSize.sp,
                                lineHeight = (baseFontSize + 6).sp,
                            ),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedContent(block.text),
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = baseFontSize.sp,
                            lineHeight = (baseFontSize + 6).sp,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                is MarkdownBlock.Divider -> {
                    // 柔和向两侧渐隐的微质感渐变分割线
                    val dividerColor = MiuixTheme.colorScheme.dividerLine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(0.75.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        dividerColor.copy(alpha = 0.35f),
                                        dividerColor.copy(alpha = 0.35f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        when {
            line.startsWith("---") || line.startsWith("***") || line.startsWith("___") -> {
                blocks.add(MarkdownBlock.Divider)
            }
            line.startsWith("#### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 4, text = line.removePrefix("#### ").trim()))
            }
            line.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 3, text = line.removePrefix("### ").trim()))
            }
            line.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(level = 2, text = line.removePrefix("## ").trim()))
            }
            line.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(level = 1, text = line.removePrefix("# ").trim()))
            }
            line.startsWith("> ") -> {
                blocks.add(MarkdownBlock.Blockquote(text = line.removePrefix("> ").trim()))
            }
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                blocks.add(MarkdownBlock.BulletItem(text = line.substring(2).trim()))
            }
            line.matches(NUMBERED_ITEM_REGEX) -> {
                val num = line.substringBefore('.')
                val content = line.substringAfter('.').trim()
                blocks.add(MarkdownBlock.NumberedItem(number = num, text = content))
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(text = line))
            }
        }
    }

    return blocks
}

fun buildAnnotatedContent(rawText: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val len = rawText.length

    while (i < len) {
        // **加粗**
        if (rawText.startsWith("**", i)) {
            val end = rawText.indexOf("**", i + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(rawText.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }

        // `代码`
        if (rawText.startsWith("`", i)) {
            val end = rawText.indexOf("`", i + 1)
            if (end != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append(rawText.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }

        append(rawText[i])
        i++
    }
}

private val NUMBERED_ITEM_REGEX = Regex("""^\d+\.\s+.*""")
