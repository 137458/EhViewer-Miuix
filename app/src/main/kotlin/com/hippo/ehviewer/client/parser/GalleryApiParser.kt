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
package com.hippo.ehviewer.client.parser

import com.ehviewer.core.database.client.GalleryMetadataParser
import com.ehviewer.core.model.GalleryInfo

// 解析实现已下沉共享层（core:data），此处保留调用点签名稳定
object GalleryApiParser {
    fun parse(body: String, galleryInfoList: List<GalleryInfo>) {
        GalleryMetadataParser.parse(body, galleryInfoList)
    }
}
