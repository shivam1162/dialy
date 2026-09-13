package com.dialy.app.core.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.dialy.app.core.util.DateUtils
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.MoodType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Result of the PDF generation operation.
 */
data class PdfExportResult(
    val uri: Uri,
    val file: File?,
    val fileName: String,
    val destinationDescription: String
)

/**
 * Generates an aesthetic PDF matching the diary stationery template (refer.jpeg)
 * with the user's filled planner data overlaid with typography and calibrated coordinates.
 */
object DiaryPdfGenerator {

    private const val PAGE_WIDTH = 1024
    private const val PAGE_HEIGHT = 1536

    // Ink and accent colors matching the reference stationery aesthetic
    private val COLOR_INK = Color.rgb(55, 40, 35)         // Warm dark chocolate charcoal
    private val COLOR_PINK_RING = Color.argb(190, 235, 140, 160) // Soft pastel pink highlight
    private val COLOR_CHECK = Color.rgb(210, 75, 105)     // Cute berry pink checkmark/hearts
    private val COLOR_HIGHLIGHT_FILL = Color.argb(160, 235, 140, 160)

    suspend fun generatePdf(context: Context, planner: DailyPlanner): Result<PdfExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "Diary_Planner_${planner.date}.pdf"

            // 1. Create native PdfDocument
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // 2. Load background template from assets
            val templateBitmap = context.assets.open("planner_template.jpg").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw IllegalStateException("Failed to decode planner template image from assets.")

            // Scale if needed to match exact page dimensions
            val scaledBitmap = if (templateBitmap.width != PAGE_WIDTH || templateBitmap.height != PAGE_HEIGHT) {
                Bitmap.createScaledBitmap(templateBitmap, PAGE_WIDTH, PAGE_HEIGHT, true)
            } else {
                templateBitmap
            }
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

            // 3. Prepare styling paints
            val casualTypeface = Typeface.create("casual", Typeface.NORMAL)

            val textMediumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK
                textSize = 16f
                typeface = casualTypeface
            }

            val textSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK
                textSize = 13.5f
                typeface = casualTypeface
            }

            val checkmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_CHECK
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val heartSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_CHECK
                textSize = 15f
                typeface = Typeface.DEFAULT
            }

            val dayHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_HIGHLIGHT_FILL
                style = Paint.Style.FILL
            }

            val moodRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PINK_RING
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }

            // 4. Render Date and Day-of-Week
            val parsedDate = DateUtils.parseIsoDate(planner.date) ?: LocalDate.now()
            val formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
            canvas.drawText(formattedDate, 105f, 230f, textMediumPaint)

            // Day circles under M T W T F S S
            val dayCircles = listOf(
                Pair(45f, 287f),   // Monday
                Pair(82f, 287f),   // Tuesday
                Pair(119f, 287f),  // Wednesday
                Pair(156f, 287f),  // Thursday
                Pair(194f, 287f),  // Friday
                Pair(232f, 287f),  // Saturday
                Pair(270f, 287f)   // Sunday
            )
            val dayIndex = (parsedDate.dayOfWeek.value - 1).coerceIn(0, 6)
            val (dayX, dayY) = dayCircles[dayIndex]
            canvas.drawCircle(dayX, dayY, 7.5f, dayHighlightPaint)

            // 5. Today's Focus
            if (planner.focus.isNotBlank()) {
                val focusText = fitText(planner.focus, 320f, textMediumPaint)
                val textWidth = textMediumPaint.measureText(focusText)
                val focusX = 505f - (textWidth / 2f)
                canvas.drawText(focusText, focusX, 248f, textMediumPaint)
            }

            // 6. Top Priorities (Lines 1, 2, 3)
            val priorityY = listOf(396f, 446f, 496f)
            val sortedPriorities = planner.topPriorities.sortedBy { it.order }
            for (i in 0 until minOf(3, sortedPriorities.size)) {
                val item = sortedPriorities[i]
                val pY = priorityY[i]
                if (item.title.isNotBlank()) {
                    val text = fitText(item.title, 220f, textMediumPaint)
                    canvas.drawText(text, 90f, pY - 14f, textMediumPaint)
                }
                if (item.isCompleted) {
                    canvas.drawText("✓", 48f, pY - 17f, checkmarkPaint)
                }
            }

            // 7. Schedule (6 AM to 10 PM)
            val schedStartY = 584f
            val schedStep = 29f
            for (hour in 6..22) {
                val idx = hour - 6
                val curY = schedStartY + (idx * schedStep)
                val hourPrefix = String.format(Locale.US, "%02d:", hour)
                val scheduleItem = planner.schedule.firstOrNull { it.timeSlot.startsWith(hourPrefix) }
                if (scheduleItem != null && scheduleItem.activity.isNotBlank()) {
                    val actText = fitText(scheduleItem.activity, 225f, textSmallPaint)
                    canvas.drawText(actText, 95f, curY - 14f, textSmallPaint)
                }
            }

            // 8. To-Do List (Up to 16 lines)
            val todoStartY = 373f
            val todoStep = 36f
            for (i in 0 until minOf(16, planner.todos.size)) {
                val todo = planner.todos[i]
                val curY = todoStartY + (i * todoStep)
                if (todo.title.isNotBlank()) {
                    val todoText = fitText(todo.title, 275f, textMediumPaint)
                    canvas.drawText(todoText, 395f, curY - 14f, textMediumPaint)
                }
                if (todo.isCompleted) {
                    canvas.drawText("✓", 362f, curY - 18f, checkmarkPaint)
                }
            }

            // 9. Notes & Ideas (Grid Box)
            if (planner.notes.isNotBlank()) {
                val noteLines = wrapText(planner.notes, 280f, textSmallPaint)
                val startNotesY = 1010f
                val lineHeight = 22f
                for (i in 0 until minOf(12, noteLines.size)) {
                    canvas.drawText(noteLines[i], 375f, startNotesY + (i * lineHeight), textSmallPaint)
                }
            }

            // 10. Self Care Habits (Hearts y positions)
            val selfCareY = listOf(382f, 416f, 450f, 484f, 518f, 552f, 586f)
            for (i in 0 until minOf(selfCareY.size, planner.selfCare.size)) {
                val habit = planner.selfCare[i]
                if (habit.isCompleted) {
                    canvas.drawText("♥", 724f, selfCareY[i] - 12f, heartSymbolPaint)
                }
            }

            // 11. Today I Am Grateful For (4 lines)
            val gratitudeY = listOf(754f, 792f, 830f, 868f)
            for (i in 0 until minOf(gratitudeY.size, planner.gratitude.size)) {
                val item = planner.gratitude[i]
                if (item.text.isNotBlank()) {
                    val text = fitText(item.text, 210f, textSmallPaint)
                    canvas.drawText(text, 748f, gratitudeY[i] - 14f, textSmallPaint)
                }
            }

            // 12. Mood Tracker
            planner.mood?.let { mood ->
                val moodFacesX = mapOf(
                    MoodType.VERY_HAPPY to 730f,
                    MoodType.HAPPY to 783f,
                    MoodType.EXCITED to 783f,
                    MoodType.CALM to 783f,
                    MoodType.NEUTRAL to 837f,
                    MoodType.TIRED to 890f,
                    MoodType.SAD to 890f,
                    MoodType.STRESSED to 943f
                )
                val faceX = moodFacesX[mood.type] ?: 783f
                // Draw cute soft pink ring around smiley face
                canvas.drawCircle(faceX, 965f, 20f, moodRingPaint)
                // Fill heart beneath face
                canvas.drawText("♥", faceX - 5.5f, 1000f, heartSymbolPaint)
            }

            // 13. End of Day Reflection
            if (planner.reflection.whatWentWell.isNotBlank()) {
                val lines = wrapText(planner.reflection.whatWentWell, 245f, textSmallPaint)
                if (lines.isNotEmpty()) canvas.drawText(lines[0], 720f, 1158f - 14f, textSmallPaint)
                if (lines.size > 1) canvas.drawText(lines[1], 720f, 1188f - 14f, textSmallPaint)
            }
            if (planner.reflection.whatCanImprove.isNotBlank()) {
                val lines = wrapText(planner.reflection.whatCanImprove, 245f, textSmallPaint)
                if (lines.isNotEmpty()) canvas.drawText(lines[0], 720f, 1276f - 14f, textSmallPaint)
                if (lines.size > 1) canvas.drawText(lines[1], 720f, 1306f - 14f, textSmallPaint)
            }
            if (planner.reflection.proudOf.isNotBlank()) {
                val lines = wrapText(planner.reflection.proudOf, 245f, textSmallPaint)
                if (lines.isNotEmpty()) canvas.drawText(lines[0], 720f, 1394f - 14f, textSmallPaint)
                if (lines.size > 1) canvas.drawText(lines[1], 720f, 1424f - 14f, textSmallPaint)
            }

            // 14. Don't Forget / Reminders (6 lines)
            val dfStartY = 1160f
            val dfStep = 36f
            for (i in 0 until minOf(6, planner.dontForget.size)) {
                val reminder = planner.dontForget[i]
                val curY = dfStartY + (i * dfStep)
                if (reminder.text.isNotBlank()) {
                    val remText = fitText(reminder.text, 190f, textMediumPaint)
                    canvas.drawText(remText, 72f, curY - 14f, textMediumPaint)
                }
                if (reminder.isCompleted) {
                    canvas.drawText("✓", 46f, curY - 18f, checkmarkPaint)
                }
            }

            // 15. Daily Reminder / Affirmation
            if (planner.dailyReminder.isNotBlank()) {
                val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(254, 248, 243)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(332f, 1420f, 565f, 1485f, 6f, 6f, patchPaint)
                val remLines = wrapText(planner.dailyReminder, 220f, textSmallPaint)
                for (i in 0 until minOf(3, remLines.size)) {
                    canvas.drawText(remLines[i], 340f, 1442f + (i * 18f), textSmallPaint)
                }
            }

            // Finish page
            pdfDocument.finishPage(page)

            // 15. Save to Cache for instant preview viewing via FileProvider (no public downloading)
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            PdfExportResult(
                uri = contentUri,
                file = cacheFile,
                fileName = fileName,
                destinationDescription = "Preview"
            )
        }
    }

    /**
     * Launches an Android system Intent to view or open the exported PDF in an external viewer.
     */
    fun openPdfViewer(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open Planner PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Truncates text with an ellipsis if it exceeds the maximum allotted width.
     */
    private fun fitText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val sub = text.substring(0, mid) + "…"
            if (paint.measureText(sub) <= maxWidth) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return text.substring(0, low) + "…"
    }

    /**
     * Splits a string into multiple lines that fit within the specified width.
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            val words = paragraph.split(" ")
            var currentLine = ""
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = candidate
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                    }
                    currentLine = fitText(word, maxWidth, paint)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
        }
        return lines
    }
}
