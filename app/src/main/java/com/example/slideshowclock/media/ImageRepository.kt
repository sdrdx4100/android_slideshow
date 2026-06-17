package com.example.slideshowclock.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enumerates image files inside a user-picked SAF tree (folder) and manages the
 * persistable read permission for it.
 */
class ImageRepository(private val context: Context) {

    /**
     * Take a long-lived read permission on [treeUri] so the folder stays accessible
     * across app restarts.
     */
    fun persistPermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(treeUri, flags) }
    }

    /** True if we still hold a read grant for [treeUri]. */
    fun hasPermission(treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }

    /**
     * List images inside the folder pointed to by [treeUri]. When [recursive] is false
     * only direct children are returned; when true, subfolders are walked depth-first
     * (folders and files each sorted by display name, so the order stays deterministic).
     * Uses [DocumentsContract] queries directly — far faster than DocumentFile.listFiles.
     */
    suspend fun listImages(treeUri: Uri, recursive: Boolean = false): List<Uri> =
        withContext(Dispatchers.IO) {
            val rootId = DocumentsContract.getTreeDocumentId(treeUri)
            val out = ArrayList<Uri>()
            collectInto(treeUri, rootId, recursive, depth = 0, out = out)
            out
        }

    /** Recursively gather image uris under [parentDocId] into [out]. */
    private fun collectInto(
        treeUri: Uri,
        parentDocId: String,
        recursive: Boolean,
        depth: Int,
        out: MutableList<Uri>,
    ) {
        if (depth > MAX_DEPTH) return
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )

        val images = ArrayList<Pair<String, Uri>>()
        val subDirs = ArrayList<Pair<String, String>>() // name to docId
        runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (c.moveToNext()) {
                    val mime = c.getString(mimeCol) ?: continue
                    val id = c.getString(idCol) ?: continue
                    val name = c.getString(nameCol) ?: id
                    when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR ->
                            if (recursive) subDirs.add(name to id)
                        mime.startsWith("image/") ->
                            images.add(name to DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
                    }
                }
            }
        }
        images.sortBy { it.first.lowercase() }
        images.forEach { out.add(it.second) }

        if (recursive) {
            subDirs.sortBy { it.first.lowercase() }
            subDirs.forEach { (_, id) -> collectInto(treeUri, id, true, depth + 1, out) }
        }
    }

    /** Best-effort display name of the picked folder, for showing in Settings. */
    fun folderDisplayName(treeUri: Uri): String? = runCatching {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        // Tree ids commonly look like "primary:Pictures/Vacation" — show the tail.
        docId.substringAfterLast(':').substringAfterLast('/').ifBlank { docId }
    }.getOrNull()

    private companion object {
        /** Guard against pathological folder trees / symlink-like loops. */
        const val MAX_DEPTH = 12
    }
}
