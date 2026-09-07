package com.cch.momentmark.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.IOException

/** Reads the packaged repository background folder without hard-coding image names. */
object BackgroundManager {
    fun list(context: Context): List<String> = runCatching {
        context.assets.list("")
            .orEmpty()
            .filter { it.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp") }
            .sorted()
    }.getOrDefault(emptyList())

    fun load(context: Context, name: String): ImageBitmap? = runCatching {
        context.assets.open(name).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()

    fun defaultName(context: Context): String? = list(context).firstOrNull()
}
