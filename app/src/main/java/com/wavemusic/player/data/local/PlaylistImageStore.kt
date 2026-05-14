package com.wavemusic.player.data.local

import android.content.Context
import android.net.Uri
import java.io.File

class PlaylistImageStore(private val context: Context) {
    fun savePlaylistCover(sourceUri: Uri, playlistId: Long): String? {
        return runCatching {
            val directory = File(context.filesDir, "playlist_covers").apply {
                if (!exists()) mkdirs()
            }
            val target = File(directory, "playlist_${playlistId}_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    fun deletePlaylistCover(imageUri: String?) {
        if (imageUri.isNullOrBlank()) return
        runCatching {
            val uri = Uri.parse(imageUri)
            if (uri.scheme == "file") {
                File(uri.path.orEmpty()).takeIf { it.exists() }?.delete()
            }
        }
    }
}

