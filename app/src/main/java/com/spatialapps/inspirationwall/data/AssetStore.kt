package com.spatialapps.inspirationwall.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class AssetStore(private val context: Context) {
    private val root = File(context.filesDir, "wall-assets").apply { mkdirs() }

    fun ensureDemoImage(id: String): String {
        val target = File(root, "$id-image.png")
        if (target.exists()) return target.absolutePath
        val bitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(213, 225, 220))
        paint.color = Color.rgb(53, 104, 89)
        canvas.drawCircle(150f, 145f, 84f, paint)
        paint.color = Color.rgb(244, 191, 111)
        canvas.drawRect(275f, 85f, 650f, 330f, paint)
        paint.color = Color.WHITE
        paint.textSize = 42f
        canvas.drawText("灵感照片", 315f, 220f, paint)
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 96, it) }
        bitmap.recycle()
        return target.absolutePath
    }

    fun importImage(id: String, source: Uri): String {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(source)
        require(mimeType == null || mimeType.startsWith("image/")) { "选择的文件不是图片" }
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val target = File(root, "$id-image.$extension")
        try {
            resolver.openInputStream(source)?.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_IMAGE_BYTES) { "图片不能超过 50 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取所选图片")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图片格式无法识别" }
            return target.absolutePath
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    fun deleteLegacyDoodles() {
        root.listFiles { file -> file.name.endsWith("-doodle.json") }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 50L * 1024L * 1024L
    }
}
