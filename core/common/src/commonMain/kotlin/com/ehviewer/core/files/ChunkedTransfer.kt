package com.ehviewer.core.files

/** Linux sendfile(2) 单次传输上限（0x7ffff000）；一次要求更多字节会被内核以 EINVAL 拒绝。 */
internal const val MAX_TRANSFER_CHUNK: Long = 0x7ffff000L

/**
 * 按 [maxChunk] 分块搬运 [total] 字节：每块以 `(offset, size)` 调用 [transfer]，返回该块实际搬运的字节数。
 *
 * sendfile/transferTo 单次调用都受内核上限约束，一次传 `Long.MAX_VALUE` 会在超过上限的输入上静默截断；
 * [transfer] 返回 0 表示源已耗尽，立即结束。
 */
internal inline fun transferInChunks(
    total: Long,
    maxChunk: Long,
    transfer: (offset: Long, size: Long) -> Long,
): Long {
    require(maxChunk > 0L) { "maxChunk must be positive: $maxChunk" }
    var copied = 0L
    while (copied < total) {
        val size = minOf(total - copied, maxChunk)
        val transferred = transfer(copied, size)
        if (transferred <= 0L) break
        copied += transferred
    }
    return copied
}
