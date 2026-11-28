/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client

/**
 * Linear-time replacements for the regular expressions that used to live in [EhUtils] for cleaning
 * up gallery titles.
 *
 * The previous implementation alternated every bracketed group with an unbounded whitespace run
 * inside an outer star quantifier. Java's regex engine implements nested quantifiers recursively
 * (one stack frame per iteration), so a title padded with a long run of whitespace overflowed the
 * stack inside the first `replaceFirst` call. The scans below perform the same transformation
 * without backtracking and stay linear in the title length.
 */
internal object EhTitleUtils {

    /** Removes leading `(...)`, `[...]`, `{...}`, `~...~` groups and whitespace runs. */
    fun stripLeading(title: String): String {
        var i = 0
        while (i < title.length) {
            val close = closeOf(title[i])
            if (close != null) {
                // `\([^)]*\)` and friends only exclude the closing character, so the group always
                // ends at the first closer that follows the opener.
                val end = title.indexOf(close, startIndex = i + 1)
                if (end < 0) break
                i = end + 1
            } else if (title[i].isWhitespace()) {
                i++
            } else {
                break
            }
        }
        return title.substring(i)
    }

    /**
     * Removes an optional trailing `ch. 1-23` style segment followed by trailing decoration groups
     * and whitespace, anchored at the end of the title.
     */
    fun stripTrailing(title: String): String {
        val length = title.length
        // decorated[i] is true when title[i until length] is a concatenation of decoration groups
        // and whitespace runs, i.e. exactly what the trailing `(?:...|\s+)*$` matched.
        val decorated = BooleanArray(length + 1)
        decorated[length] = true
        // Closest occurrence of each character strictly to the right of the current index.
        val closerIndex = HashMap<Char, Int>(4)
        for (i in length - 1 downTo 0) {
            val char = title[i]
            val close = closeOf(char)
            var matches = close != null && closerIndex[close]?.let { decorated[it + 1] } == true
            if (!matches && char.isWhitespace()) {
                matches = decorated[i + 1]
            }
            decorated[i] = matches
            closerIndex[char] = i
        }
        // Leftmost start position wins, matching `replaceFirst` semantics.
        var start = decorated.indexOfFirst { it }
        val chapterStart = findChapterSegmentStart(title, decorated)
        if (chapterStart != null && chapterStart < start) {
            start = chapterStart
        }
        return title.substring(0, start)
    }

    /** Removes leading and trailing decorations, equivalent to the old two-step `replaceFirst`. */
    fun stripDecorations(title: String): String = stripTrailing(stripLeading(title))

    private fun findChapterSegmentStart(title: String, decorated: BooleanArray): Int? {
        val length = title.length
        var i = 0
        while (i < length) {
            if (!title[i].isWhitespace()) {
                i++
                continue
            }
            var whitespaceEnd = i
            while (whitespaceEnd < length && title[whitespaceEnd].isWhitespace()) {
                whitespaceEnd++
            }
            // `\s+ch.` needs the two marker characters plus the single character matched by `.`
            if (whitespaceEnd + 3 <= length &&
                title[whitespaceEnd].lowercaseChar() == 'c' &&
                title[whitespaceEnd + 1].lowercaseChar() == 'h' &&
                title[whitespaceEnd + 2] != '\n' &&
                title[whitespaceEnd + 2] != '\r'
            ) {
                var end = whitespaceEnd + 3
                while (end < length && isChapterNumber(title[end])) {
                    end++
                }
                // `[\s\d-]+` requires at least one character after the marker.
                if (end > whitespaceEnd + 3 && decorated[end]) {
                    return i
                }
            }
            i = whitespaceEnd
        }
        return null
    }

    private fun closeOf(char: Char): Char? = when (char) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        '~' -> '~'
        else -> null
    }

    private fun isChapterNumber(char: Char): Boolean = char.isWhitespace() || char.isDigit() || char == '-'
}
