package com.gestureNav.gesture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {

    private var stream: OutputStream? = null
    private var fileUri: Uri? = null
    private var contentResolverContext: Context? = null

    fun start(context: Context) {
        val fileName = "gesturenav_debug_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            fileUri = uri
            contentResolverContext = context.applicationContext
            stream = uri?.let { resolver.openOutputStream(it) }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            stream = FileOutputStream(file)
        }
    }

    fun log(line: String) {
        try {
            stream?.write((line + "\n").toByteArray())
            stream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            stream?.flush()
            stream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stream = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = fileUri
            val context = contentResolverContext
            if (uri != null && context != null) {
                val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, values, null, null)
            }
        }
        fileUri = null
        contentResolverContext = null
    }
}

