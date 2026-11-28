package com.ehviewer.core.files

import okio.Path

/**
 * 由 SAF 树文档 URI 的路径段重建 document id。
 *
 * `encodedSegments` 形如 `["tree", <树 id>, "document", <document id 片段…>]`，各段仍带百分号编码。
 * document id 在 URI 里是单段，内部 `/` 被编码成 `%2F`，所以各段必须先解码再以 `/` 拼接；
 * 直接拼编码后的段会让 appendPath 再编码一次（`%23` 变成 `%2523`）。
 *
 * @return 已解码的 document id；`null` 表示不是树中文档 URI，调用方应原样使用原 URI。
 */
internal fun rebuildTreeDocumentId(encodedSegments: List<String>, decode: (String) -> String): String? {
    if (encodedSegments.size <= 4 || encodedSegments[0] != "tree") return null
    val decoded = encodedSegments.subList(3, encodedSegments.size).map(decode)
    val root = decoded.first()
    val rest = decoded.subList(1, decoded.size)
    // 卷根 id 形如 "primary:"，自身已带分隔符，再补 '/' 会拼出 "primary://sub"
    val prefix = if (root.endsWith(':')) root else "$root/"
    return rest.joinToString("/", prefix)
}

/**
 * 子项显示名能否安全地拼进路径。
 *
 * SAF 提供器返回的 COLUMN_DISPLAY_NAME 不可信：`..` 会让后续 resolve 逃出父目录，
 * 含 `/` 的名字会被当成多级路径。
 */
internal fun isSafeChildName(name: String): Boolean = name.isNotEmpty() && name != "." && name != ".." && '/' !in name

/** 两个路径是否同父目录：DocumentsContract.renameDocument 只能在同一父目录内改名。 */
internal fun isSameParentDirectory(source: Path, target: Path): Boolean {
    val sourceParent = source.parent ?: return false
    val targetParent = target.parent ?: return false
    return sourceParent.toString() == targetParent.toString()
}
