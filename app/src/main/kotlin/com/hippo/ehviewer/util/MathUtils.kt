package com.hippo.ehviewer.util

// a == b 时区间为空，(pos - a) / 0 会得到 NaN/Infinity 并污染布局计算，直接返回 0
fun calculateFraction(a: Float, b: Float, pos: Float) = if (a == b) 0f else ((pos - a) / (b - a)).coerceIn(0f, 1f)
