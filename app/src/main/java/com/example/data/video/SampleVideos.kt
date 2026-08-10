package com.example.data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SampleVideoModel(
    val id: String,
    val title: String,
    val category: String,
    val durationMs: Long,
    val thumbnailColor: Long,
    val description: String,
    val initialBurmeseNarrative: String
)

object SampleVideos {
    val presets = listOf(
        SampleVideoModel(
            id = "sample_cyberpunk",
            title = "Neo-Yangon 2088 Heist",
            category = "Sci-Fi Thriller",
            durationMs = 30000L,
            thumbnailColor = 0xFF1A0B2E,
            description = "High-tech cyberpunk heist in neon-lit rain.",
            initialBurmeseNarrative = "နီယွန်မီးရောင်တွေ မှိန်ပျပျ လင်းလက်နေတဲ့ ၂၀၈၈ မြို့တော်မှာ အကြီးမားဆုံး ဟက်ကာအဖွဲ့ရဲ့ စမတ်ကျတဲ့ စဟန်..."
        ),
        SampleVideoModel(
            id = "sample_martial_arts",
            title = "Shadow Blade Legend",
            category = "Action Wuxia",
            durationMs = 25000L,
            thumbnailColor = 0xFF2D0B0F,
            description = "Ancient swordmasters showdown under the full moon.",
            initialBurmeseNarrative = "ရှေးဟောင်း ဓားသိုင်းလောက၏ လဆန်းညတစ်ညတွင် နှစ်ပေါင်းများစွာ လျှို့ဝှက်ထားခဲ့သော ရှေးဟောင်း ဓားကျမ်း..."
        ),
        SampleVideoModel(
            id = "sample_space",
            title = "Galaxy Frontier Explorer",
            category = "Sci-Fi Adventure",
            durationMs = 35000L,
            thumbnailColor = 0xFF0B1D2E,
            description = "Interstellar vessel discovering an unmapped wormhole.",
            initialBurmeseNarrative = "စကြာဝဠာ၏ အနက်ရှိုင်းဆုံး အာကာသနယ်မြေဆီသို့ ဦးတည်မောင်းနှင်နေသော အာကာသယာဉ်ကြီး၏ သမိုင်းဝင်..."
        )
    )

    /**
     * Generates a sample video poster thumbnail or frame sequence image file
     */
    suspend fun createSampleThumbnail(
        context: Context,
        model: SampleVideoModel
    ): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "thumb_${model.id}.jpg")
        if (file.exists() && file.length() > 0) return@withContext file

        try {
            val bitmap = Bitmap.createBitmap(800, 450, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint().apply {
                color = model.thumbnailColor.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 800f, 450f, bgPaint)

            // Draw movie title & badge on poster canvas
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 36f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val subPaint = Paint().apply {
                color = Color.parseColor("#FFB703")
                textSize = 24f
                isAntiAlias = true
            }

            canvas.drawText(model.title, 50f, 200f, textPaint)
            canvas.drawText("Recap Studio Demo • ${model.category}", 50f, 250f, subPaint)

            // Draw clapperboard watermark
            val borderPaint = Paint().apply {
                color = Color.parseColor("#33FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }
            canvas.drawRoundRect(RectF(30f, 30f, 770f, 420f), 16f, 16f, borderPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            Log.e("SampleVideos", "Error creating thumbnail: ${e.message}", e)
        }
        file
    }
}
