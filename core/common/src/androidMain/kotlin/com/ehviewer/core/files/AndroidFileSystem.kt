package com.ehviewer.core.files

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Int64Ref
import android.system.Os
import android.webkit.MimeTypeMap
import androidx.core.database.getLongOrNull
import kotlinx.io.asSink
import kotlinx.io.asSource
import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source
import okio.sink
import okio.source

class AndroidFileSystem(context: Context) : FileSystem() {
    private val contentResolver = context.contentResolver
    private val physicalFileSystem = SYSTEM

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        if (file.isPhysicalFile()) {
            return physicalFileSystem.appendingSink(file, mustExist)
        }
        if (mustExist && !exists(file)) {
            throw FileNotFoundException("$file does not exist")
        }
        return ParcelFileDescriptor.AutoCloseOutputStream(openFileDescriptor(file, "wa")).sink()
    }

    override fun atomicMove(source: Path, target: Path) {
        if (source.isPhysicalFile()) {
            return physicalFileSystem.atomicMove(source, target)
        }

        source.runCatching {
            DocumentsContract.renameDocument(contentResolver, toUri(), target.name)
        }.onFailure {
            // ExternalStorageProvider always throw exception when renameDocument on API 28
            // https://android.googlesource.com/platform/frameworks/base/+/7bf90408e36613a84dc2a665905fde2c83cfa797
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
                throw FileNotFoundException("Failed to move $source to $target")
            }
        }
    }

    override fun canonicalize(path: Path): Path = if (path.isPhysicalFile()) physicalFileSystem.canonicalize(path) else path

    override fun copy(source: Path, target: Path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            source.openFileDescriptor("r").use { src ->
                target.openFileDescriptor("wt").use { dst ->
                    try {
                        Os.sendfile(dst.fileDescriptor, src.fileDescriptor, Int64Ref(0), Long.MAX_VALUE)
                        return
                    } catch (_: ErrnoException) {}
                }
            }
        }

        // Fallback to transferTo if sendfile is not available or fails
        source.inputStream().use { src ->
            target.outputStream().use { dst ->
                src.channel.transferTo(0, Long.MAX_VALUE, dst.channel)
            }
        }
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        if (dir.isPhysicalFile()) {
            return physicalFileSystem.createDirectory(dir, mustCreate)
        }

        val alreadyExist = metadataOrNull(dir)?.isDirectory == true
        if (alreadyExist) {
            if (mustCreate) {
                throw IOException("$dir already exist")
            } else {
                return
            }
        }

        dir.parent?.runCatching {
            DocumentsContract.createDocument(contentResolver, toUri(), Document.MIME_TYPE_DIR, dir.name)
        }?.getOrNull() ?: throw IOException("Failed to create directory: $dir")
    }

    override fun createSymlink(source: Path, target: Path) {
        if (source.isPhysicalFile() && target.isPhysicalFile()) {
            return physicalFileSystem.createSymlink(source, target)
        }
        throw IOException("Symbolic links are not supported for document URIs")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (path.isPhysicalFile()) {
            return physicalFileSystem.delete(path, mustExist)
        }

        val metadata = metadataOrNull(path)

        if (metadata != null) {
            var uri = path.toUri()
            if (uri.isCifsDocument() && metadata.isDirectory) {
                uri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getDocumentId(uri) + '/')
            }

            val deleted = runCatching {
                DocumentsContract.deleteDocument(contentResolver, uri)
            }.getOrDefault(false)
            if (!deleted) {
                throw IOException("Failed to delete $path")
            }
        } else if (mustExist) {
            throw FileNotFoundException("$path does not exist")
        }
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
        if (fileOrDirectory.isPhysicalFile()) {
            if (metadataOrNull(fileOrDirectory)?.isDirectory == true) {
                physicalFileSystem.deleteRecursively(fileOrDirectory, mustExist)
            } else {
                physicalFileSystem.delete(fileOrDirectory, mustExist)
            }
        } else {
            delete(fileOrDirectory, mustExist)
        }
    }

    override fun list(dir: Path): List<Path> = list(dir, throwOnFailure = true)!!

    override fun listOrNull(dir: Path): List<Path>? = list(dir, throwOnFailure = false)

    private fun list(dir: Path, throwOnFailure: Boolean): List<Path>? {
        if (dir.isPhysicalFile()) {
            return if (throwOnFailure) {
                physicalFileSystem.list(dir)
            } else {
                physicalFileSystem.listOrNull(dir)
            }
        }

        return runCatching {
            val uri = dir.toUri()
            var documentId = DocumentsContract.getDocumentId(uri)
            if (uri.isCifsDocument()) {
                documentId += '/'
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)

            contentResolver.query(childrenUri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                List(c.count) {
                    c.moveToNext()
                    val displayName = c.getString(0)
                    dir / displayName
                }
            }
        }.getOrElse { if (throwOnFailure) throw FileNotFoundException("Failed to list $dir") else null }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (path.isPhysicalFile()) {
            return physicalFileSystem.metadataOrNull(path)
        }

        return runCatching {
            val uri = path.toUri()
            val isMediaUri = uri.authority == MediaStore.AUTHORITY
            val projection = if (isMediaUri) {
                arrayOf(MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_MODIFIED)
            } else {
                arrayOf(Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED)
            }

            contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (!c.moveToNext()) return null

                val mimeType = c.getString(0)
                val lastModified = c.getLongOrNull(1)?.let { if (isMediaUri) it * 1000 else it }
                val isDirectory = mimeType == Document.MIME_TYPE_DIR

                FileMetadata(
                    isRegularFile = !isDirectory,
                    isDirectory = isDirectory,
                    lastModifiedAtMillis = lastModified,
                )
            }
        }.getOrNull()
    }

    override fun openReadOnly(file: Path): FileHandle {
        if (file.isPhysicalFile()) return physicalFileSystem.openReadOnly(file)
        return openHandle(file, readWrite = false)
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        if (file.isPhysicalFile()) {
            return physicalFileSystem.openReadWrite(file, mustCreate, mustExist)
        }
        val present = exists(file)
        if (present && mustCreate) throw IOException("$file already exists")
        if (!present && mustExist) throw FileNotFoundException("$file does not exist")
        return openHandle(file, readWrite = true)
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        if (file.isPhysicalFile()) return physicalFileSystem.sink(file, mustCreate)
        if (mustCreate && exists(file)) throw IOException("$file already exists")
        return ParcelFileDescriptor.AutoCloseOutputStream(openFileDescriptor(file, "wt")).sink()
    }

    override fun source(file: Path): Source {
        if (file.isPhysicalFile()) return physicalFileSystem.source(file)
        return ParcelFileDescriptor.AutoCloseInputStream(openFileDescriptor(file, "r")).source()
    }

    private fun openHandle(
        file: Path,
        readWrite: Boolean,
    ): FileHandle {
        val descriptor = openFileDescriptor(file, if (readWrite) "rw" else "r")
        val fd = descriptor.fileDescriptor
        return object : FileHandle(readWrite) {
            override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int {
                // Okio's FileHandle contract reports end-of-file as -1, while pread(2) reports it
                // as 0. Returning 0 makes FileHandle.read loop forever without consuming input.
                val read = Os.pread(fd, array, arrayOffset, byteCount, fileOffset)
                return if (read == 0 && byteCount > 0) -1 else read
            }

            override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) {
                check(readWrite)
                // pread/pwrite may transfer fewer bytes than requested; Okio cannot retry because
                // its contract returns Unit, so a short write has to be completed here.
                var written = 0
                while (written < byteCount) {
                    val count = Os.pwrite(fd, array, arrayOffset + written, byteCount - written, fileOffset + written)
                    if (count <= 0) throw IOException("Short write to $file: $written of $byteCount bytes")
                    written += count
                }
            }

            override fun protectedFlush() {
                if (readWrite) Os.fsync(fd)
            }

            override fun protectedResize(size: Long) {
                check(readWrite)
                Os.ftruncate(fd, size)
            }

            override fun protectedSize(): Long = Os.fstat(fd).st_size

            override fun protectedClose() {
                descriptor.close()
            }
        }
    }

    fun rawSink(file: Path) = file.outputStream().asSink()

    fun rawSource(file: Path) = file.inputStream().asSource()

    fun openFileDescriptor(path: Path, mode: String): ParcelFileDescriptor {
        if (path.isPhysicalFile()) {
            return ParcelFileDescriptor.open(path.toFile(), ParcelFileDescriptor.parseMode(mode))
        }

        return runCatching {
            val uri = if ('w' in mode && !exists(path)) {
                val parent = path.parent ?: return@runCatching null
                val displayName = path.name
                val extension = displayName.substringAfterLast('.', "").ifEmpty { null }?.lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
                DocumentsContract.createDocument(contentResolver, parent.toUri(), mimeType, displayName)
                    ?: return@runCatching null
            } else {
                path.toUri()
            }
            contentResolver.openFileDescriptor(uri, mode)
        }.getOrNull() ?: throw FileNotFoundException("Failed to open file: $path")
    }

    private fun Path.inputStream() = ParcelFileDescriptor.AutoCloseInputStream(openFileDescriptor(this, "r"))

    private fun Path.outputStream() = ParcelFileDescriptor.AutoCloseOutputStream(openFileDescriptor(this, "wt"))
}

private fun Path.isPhysicalFile() = toString().startsWith('/')

private fun Uri.isCifsDocument() = authority == "com.wa2c.android.cifsdocumentsprovider.documents"
