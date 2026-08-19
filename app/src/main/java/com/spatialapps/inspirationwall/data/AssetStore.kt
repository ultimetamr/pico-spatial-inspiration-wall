package com.spatialapps.inspirationwall.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

class AssetStore(context: Context) {
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

    fun saveDoodle(id: String, pointsJson: String): String {
        val target = File(root, "$id-doodle.json")
        target.writeText(pointsJson)
        return target.absolutePath
    }
}
