package com.ehviewer.core.files

import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import okio.FileSystem
import okio.Path

actual val SystemFileSystem: FileSystem = FileSystem.SYSTEM

actual inline fun <T> Path.read(f: Source.() -> T): T = FileInputStream(toFile()).asSource().buffered().use(f)

actual inline fun <T> Path.write(f: Sink.() -> T): T = FileOutputStream(toFile()).asSink().buffered().use(f)
