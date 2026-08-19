package com.spatialapps.inspirationwall.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.provider.MediaStore
import com.spatialapps.inspirationwall.data.CardEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WallExporter {
    fun export(context: Context, wallName: String, cards: List<CardEntity>): Result<String> = runCatching {
        val bitmap = Bitmap.createBitmap(2048, 1152, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(238, 232, 218))
        paint.color = Color.rgb(49, 55, 51)
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText(wallName, 84f, 92f, paint)
        paint.textSize = 24f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(93, 101, 94)
        canvas.drawText("桌面空间灵感墙 · 完整布局导出", 86f, 132f, paint)

        val minX = cards.minOfOrNull { it.x } ?: 0f
        val maxX = cards.maxOfOrNull { it.x + it.width * it.scale } ?: 1000f
        val minY = cards.minOfOrNull { it.y } ?: 0f
        val maxY = cards.maxOfOrNull { it.y + it.height * it.scale } ?: 650f
        val scale = minOf(1800f / (maxX - minX).coerceAtLeast(900f), 900f / (maxY - minY).coerceAtLeast(560f))
        cards.sortedBy { it.zIndex }.forEach { card ->
            canvas.save()
            val left = 100f + (card.x - minX) * scale
            val top = 178f + (card.y - minY) * scale
            val width = card.width * card.scale * scale
            val height = card.height * card.scale * scale
            canvas.rotate(card.rotation, left + width / 2, top + height / 2)
            paint.setShadowLayer(12f, 4f, 6f, Color.argb(50, 0, 0, 0))
            paint.color = PAPER_COLORS[card.paperStyle.mod(PAPER_COLORS.size)]
            canvas.drawRoundRect(RectF(left, top, left + width, top + height), 16f, 16f, paint)
            paint.clearShadowLayer()
            canvas.save()
            canvas.clipRect(left + 10f, top + 10f, left + width - 10f, top + height - 10f)
            paint.color = Color.rgb(55, 57, 51)
            paint.textSize = (24f * scale).coerceIn(18f, 32f)
            paint.isFakeBoldText = true
            canvas.drawText(card.title.take(18), left + 20f, top + 42f, paint)
            paint.textSize = (17f * scale).coerceIn(14f, 24f)
            paint.isFakeBoldText = false
            card.content.chunked(22).take(3).forEachIndexed { index, line ->
                canvas.drawText(line, left + 20f, top + 78f + index * 29f, paint)
            }
            canvas.restore()
            canvas.restore()
        }
        val name = "InspirationWall_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/InspirationWall")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建导出文件")
        context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: error("无法写入导出文件")
        bitmap.recycle()
        uri.toString()
    }

    private val PAPER_COLORS = intArrayOf(
        Color.rgb(251, 248, 238), Color.rgb(249, 234, 163),
        Color.rgb(247, 216, 221), Color.rgb(211, 229, 238),
    )
}
