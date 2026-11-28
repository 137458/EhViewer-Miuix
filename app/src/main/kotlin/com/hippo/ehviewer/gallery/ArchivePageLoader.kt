/*
 * Copyright 2023-2024 Tarsin Norbin
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
package com.hippo.ehviewer.gallery

import arrow.autoCloseScope
import com.ehviewer.core.files.openFileDescriptor
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.util.logcat
import com.hippo.ehviewer.Settings.archivePasswds
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.image.ImageSource
import com.hippo.ehviewer.image.byteBufferSource
import com.hippo.ehviewer.jni.closeArchive
import com.hippo.ehviewer.jni.extractToByteBuffer
import com.hippo.ehviewer.jni.extractToFd
import com.hippo.ehviewer.jni.getExtension
import com.hippo.ehviewer.jni.needPassword
import com.hippo.ehviewer.jni.openArchive
import com.hippo.ehviewer.jni.providePassword
import com.hippo.ehviewer.jni.releaseByteBuffer
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.displayName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.kt.install
import okio.Path

typealias PasswdInvalidator = (String) -> Boolean
typealias PasswdProvider = suspend (PasswdInvalidator) -> String

// native 侧 archive 是全局状态（openArchive/closeArchive 都不带句柄），两个 gallery 同时打开会互相踩掉，
// 因此整个 archive 生命周期用一把全局 Mutex 串行化。
@PublishedApi
internal val archiveLifecycleMutex = Mutex()

suspend inline fun <T> useArchivePageLoader(
    file: Path,
    info: GalleryInfo? = null,
    startPage: Int = 0,
    hasAds: Boolean = false,
    crossinline passwdProvider: PasswdProvider,
    crossinline block: suspend (PageLoader) -> T,
) = archiveLifecycleMutex.withLock {
    autoCloseScope {
        coroutineScope {
            val pfd = install(file.openFileDescriptor("r"))
            val size = install(
                { openArchive(pfd.fd, pfd.statSize, info == null || file.name.endsWith(".zip")) },
                { _, _ -> closeArchive() },
            )
            check(size > 0) { "Archive have no content!" }
            if (needPassword() && archivePasswds.none(::providePassword)) {
                archivePasswds += passwdProvider(::providePassword)
            }
            val loader = install(
                object : PageLoader(this, info, startPage, size, hasAds) {
                    override val title by lazy {
                        if (info != null) {
                            EhUtils.getSuitableTitle(info)
                        } else {
                            // dotfile 之类解析不出名字时退回原始文件名，别对用户抛 NPE
                            FileUtils.getNameFromFilename(file.displayName) ?: file.displayName
                        }
                    }

                    override fun getImageExtension(index: Int) = getExtension(index)

                    override fun save(index: Int, file: Path) = runCatching {
                        file.openFileDescriptor("w").use {
                            extractToFd(index, it.fd)
                        }
                    }.getOrElse {
                        logcat(it)
                        false
                    }

                    override fun openSource(index: Int): ImageSource {
                        val buffer = extractToByteBuffer(index)
                        checkNotNull(buffer) { "Extract archive content $index failed!" }
                        check(buffer.isDirect)
                        return byteBufferSource(buffer) { releaseByteBuffer(buffer) }
                    }

                    override fun isSourceAvailable(index: Int) = true

                    override fun prefetchPages(pages: List<Int>, bounds: IntRange) = Unit

                    override fun onRequest(index: Int, force: Boolean, orgImg: Boolean) = notifySourceReady(index)
                },
            )
            block(loader)
        }
    }
}
