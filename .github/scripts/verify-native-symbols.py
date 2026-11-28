#!/usr/bin/env python3
"""校验 libehviewer.so 的 JNI 导出面。

入库的预编译 .so 必须与 Kotlin 侧 external fun 声明对齐：任一缺失都会在运行到对应调用时
抛 UnsatisfiedLinkError。这里直接解析 ELF64 动态符号表，不依赖 nm/readelf，因此各平台都能跑。
"""

import struct
import sys

SHT_DYNSYM = 11
SHN_UNDEF = 0

# 与 app/build.gradle.kts 的 checkNativeLibs 保持同一份清单
REQUIRED_SYMBOLS = [
    "JNI_OnLoad",
    # app/src/main/kotlin/com/hippo/ehviewer/jni/Archive.kt
    "Java_com_hippo_ehviewer_jni_ArchiveKt_openArchive",
    "Java_com_hippo_ehviewer_jni_ArchiveKt_closeArchive",
    "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteOpen",
    "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteEntry",
    "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteClose",
    # app/src/main/kotlin/com/hippo/ehviewer/jni/GifUtils.kt
    "Java_com_hippo_ehviewer_jni_GifUtilsKt_rewriteGifSource",
    # app/src/main/kotlin/com/hippo/ehviewer/image/Image.kt
    "Java_com_hippo_ehviewer_image_ImageKt_detectBorder",
    "Java_com_hippo_ehviewer_image_ImageKt_copyBitmapToAHB",
    # app/src/main/kotlin/com/hippo/ehviewer/client/parser/GalleryListParser.kt
    "Java_com_hippo_ehviewer_client_parser_GalleryListParserKt_parseGalleryInfoList",
]


def defined_dynamic_symbols(data: bytes) -> set:
    if data[:4] != b"\x7fELF":
        raise SystemExit("不是 ELF 文件")
    if data[4] != 2 or data[5] != 1:
        raise SystemExit("只支持 ELF64 小端产物")

    (e_shoff,) = struct.unpack_from("<Q", data, 0x28)
    (e_shentsize,) = struct.unpack_from("<H", data, 0x3A)
    (e_shnum,) = struct.unpack_from("<H", data, 0x3C)

    symbols = set()
    for index in range(e_shnum):
        header = e_shoff + index * e_shentsize
        (sh_type,) = struct.unpack_from("<I", data, header + 4)
        if sh_type != SHT_DYNSYM:
            continue
        (sh_offset,) = struct.unpack_from("<Q", data, header + 0x18)
        (sh_size,) = struct.unpack_from("<Q", data, header + 0x20)
        (sh_link,) = struct.unpack_from("<I", data, header + 0x28)
        (strtab_offset,) = struct.unpack_from("<Q", data, e_shoff + sh_link * e_shentsize + 0x18)
        for offset in range(sh_offset, sh_offset + sh_size, 24):
            (st_name,) = struct.unpack_from("<I", data, offset)
            (st_shndx,) = struct.unpack_from("<H", data, offset + 6)
            if st_name == 0 or st_shndx == SHN_UNDEF:
                continue
            start = strtab_offset + st_name
            end = data.find(b"\x00", start)
            if end < 0:
                end = len(data)
            symbols.add(data[start:end].decode("utf-8", "replace"))
    return symbols


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit(f"用法: {sys.argv[0]} <libehviewer.so>")

    with open(sys.argv[1], "rb") as library:
        exported = defined_dynamic_symbols(library.read())

    missing = [symbol for symbol in REQUIRED_SYMBOLS if symbol not in exported]
    if missing:
        print(f"FAIL {sys.argv[1]} 缺少 JNI 导出符号 {len(missing)} 个：")
        for symbol in missing:
            print(f"  - {symbol}")
        print("入库 .so 与 Kotlin 侧 external fun 声明脱钩，请带 -PbuildNative 重编并提交新 .so。")
        return 1

    print(f"OK {sys.argv[1]} 导出 {len(exported)} 个动态符号，必需 JNI 符号全部存在。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
