package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.database.MessageEntity
import com.example.data.database.UserEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private const val TAG = "ReportExporter"

    class HealthDiagnosis(
        val chiefComplaints: List<String>,
        val suggestionsList: List<String>
    )

    /**
     * Filters out standard conversational chit-chat, greetings, and generic closings ("fazool chat").
     * Separates the patient's physical symptoms / complaints and pairs them with Dr SAM's clinical guidance.
     */
    private fun parseHealthProblemsAndSolutions(messages: List<MessageEntity>): HealthDiagnosis {
        val complaints = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        val greetings = setOf(
            "hi", "hello", "hey", "dr sam", "thanks", "thank you", "ok", "yes", "no", "bye", 
            "good morning", "good afternoon", "good evening", "please help", "help", "how are you", 
            "hy", "hlo", "dear", "sir", "doctor", "please help me", "okay", "alright", "great"
        )

        messages.forEach { msg ->
            val content = msg.content.trim()
            if (msg.sender == "user") {
                val cleanLine = content.lowercase().replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
                if (cleanLine.length > 5 && !greetings.contains(cleanLine)) {
                    complaints.add(content)
                }
            } else {
                // Parse AI messages, filtering out welcome/intro & heavy repetitive boilerplate
                val lines = content.split("\n")
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isNotBlank()) {
                        val lower = line.lowercase()
                        val isGreeting = lower.startsWith("hello") || lower.startsWith("hi ") || lower.startsWith("welcome") || lower.startsWith("how can i help") || lower.startsWith("i'm sorry to hear") || lower.startsWith("i am sorry to hear") || lower.startsWith("based on your report")
                        val isDisclaimer = lower.contains("licensed medical doctor") || lower.contains("ai assistant") || lower.contains("not a licensed")
                        val isClosing = lower.contains("feel free to ask") || lower.contains("hope this helps") || lower.contains("seek professional") || lower.contains("always prioritize") || lower.startsWith("if you have emergency")

                        if (!isGreeting && !isDisclaimer && !isClosing && line.length > 10) {
                            suggestions.add(line)
                        }
                    }
                }
            }
        }

        // Logical Fallbacks
        if (complaints.isEmpty()) {
            complaints.add("General wellness check. Patient initiated consultation with no acute symptoms expressed.")
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Conducted clinical consultation on wellness habits. Focus on strict hydration, clean sleep, and light nutrition.")
        }

        return HealthDiagnosis(complaints, suggestions)
    }

    private fun getReportsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Here is your compiled clinical health report from Dr SAM.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save or Send Health Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Compiles a highly readable plain-text medical health report with chit-chat filtered.
     */
    fun exportChatAsText(context: Context, chatTitle: String, user: UserEntity, messages: List<MessageEntity>) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "Cannot export an empty conversation.", Toast.LENGTH_SHORT).show()
            return
        }

        val diagnosis = parseHealthProblemsAndSolutions(messages)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())

        val stringBuilder = StringBuilder()
        stringBuilder.append("============================================================\n")
        stringBuilder.append("                DR SAM — CLINICAL HEALTH REPORT             \n")
        stringBuilder.append("============================================================\n\n")
        stringBuilder.append("Report Compiled: $formattedDate (UTC)\n")
        stringBuilder.append("Patient Name:    ${user.name}\n")
        if (!user.dob.isNullOrBlank()) stringBuilder.append("Patient DOB:     ${user.dob}\n")
        if (!user.gender.isNullOrBlank()) stringBuilder.append("Gender:          ${user.gender}\n")
        stringBuilder.append("Topic discussed: $chatTitle\n")
        stringBuilder.append("------------------------------------------------------------\n")
        stringBuilder.append("IMPORTANT LIABILITY DISCLAIMER:\n")
        stringBuilder.append("Dr SAM is an AI assistant and not a licensed medical doctor. All\n")
        stringBuilder.append("responses are educational, suggest precautions, or OTC pain\n")
        stringBuilder.append("remedies carefully. Consult a physician immediately for emergencies.\n")
        stringBuilder.append("============================================================\n\n")
        
        stringBuilder.append("1. CLINICAL SYMPTOMS & CHIEF COMPLAINTS (Extracted)\n")
        stringBuilder.append("------------------------------------------------------------\n")
        diagnosis.chiefComplaints.forEach { complaint ->
            stringBuilder.append("• ").append(complaint).append("\n")
        }
        stringBuilder.append("\n")

        stringBuilder.append("2. DR SAM'S RECOMMENDATIONS & THERAPEUTIC SOLUTIONS\n")
        stringBuilder.append("------------------------------------------------------------\n")
        diagnosis.suggestionsList.forEach { solution ->
            stringBuilder.append("").append(solution).append("\n")
        }
        stringBuilder.append("\n")

        stringBuilder.append("------------------------------------------------------------\n")
        stringBuilder.append("End of Clinical Consultation. Always prioritize real doctor advice.\n")
        stringBuilder.append("============================================================\n")

        try {
            val fileName = "Dr_SAM_Report_${System.currentTimeMillis()}.txt"
            val file = File(getReportsDirectory(context), fileName)
            FileOutputStream(file).use { out ->
                out.write(stringBuilder.toString().toByteArray())
            }
            shareFile(context, file, "text/plain", "Dr SAM Health Report - $chatTitle")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to compile text report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Compiles and renders a beautiful multi-paged, high-fidelity PDF report with zero conversational noise.
     */
    fun exportChatAsPdf(context: Context, chatTitle: String, user: UserEntity, messages: List<MessageEntity>) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "Cannot export an empty conversation.", Toast.LENGTH_SHORT).show()
            return
        }

        val diagnosis = parseHealthProblemsAndSolutions(messages)
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 dimensions in postscript points
        val pageHeight = 842
        var pageNumber = 1

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        val marginX = 40f
        val maxContentWidth = pageWidth - (2 * marginX)
        var currentY = 40f

        val brandPaint = TextPaint().apply {
            color = Color.parseColor("#2563EB") // Sleek Branded Blue
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        fun addPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(info)
            canvas = currentPage.canvas
            currentY = 40f

            // Add subtle header to new pages
            brandPaint.textSize = 8f
            brandPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText("Dr SAM — Clinical Consultation Report (Page $pageNumber)", marginX, 25f, brandPaint)
            
            canvas.drawLine(marginX, 30f, pageWidth - marginX, 30f, linePaint)
            currentY = 45f
        }

        fun drawParagraph(text: String, isBold: Boolean = false, isItalic: Boolean = false, size: Float = 10f, colorHex: String = "#1E293B"): Float {
            val paint = TextPaint().apply {
                isAntiAlias = true
                textSize = size
                color = Color.parseColor(colorHex)
                val style = when {
                    isBold && isItalic -> Typeface.BOLD_ITALIC
                    isBold -> Typeface.BOLD
                    isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                typeface = Typeface.create(Typeface.DEFAULT, style)
            }

            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, maxContentWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1.1f, 1.0f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, paint, maxContentWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.0f, false)
            }

            val blockHeight = staticLayout.height.toFloat()
            if (currentY + blockHeight > pageHeight - 50f) {
                addPage()
            }

            canvas.save()
            canvas.translate(marginX, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            val writtenHeight = blockHeight
            currentY += writtenHeight + 4f
            return writtenHeight
        }

        // --- Render Document ---

        // 1. Draw elegant Header Banner
        val accentBgPaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.FILL
        }
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 45f, accentBgPaint)

        brandPaint.color = Color.WHITE
        brandPaint.textSize = 14f
        brandPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Dr SAM — Clinical Health Report", marginX + 15f, currentY + 28f, brandPaint)

        currentY += 55f

        // 2. Patient / Report Details Cards
        drawParagraph("PATIENT DIAGNOSTIC LOG DETAILS", isBold = true, size = 11f, colorHex = "#2563EB")
        
        val sysFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateString = sysFormat.format(Date())

        drawParagraph("Compilation Date: $dateString (UTC)")
        drawParagraph("Patient Name:     ${user.name}")
        if (!user.dob.isNullOrBlank()) drawParagraph("Date of Birth:    ${user.dob}")
        if (!user.gender.isNullOrBlank()) drawParagraph("Gender Profile:   ${user.gender}")
        drawParagraph("Assessment ID:    $chatTitle")

        currentY += 8f

        // 3. Disclaimer Box
        val disBlockHeight = 54f
        if (currentY + disBlockHeight > pageHeight - 50f) {
            addPage()
        }

        val boxPaint = Paint().apply {
            color = Color.parseColor("#EFF6FF")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#BFDBFE")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + disBlockHeight, boxPaint)
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + disBlockHeight, borderPaint)

        currentY += 6f
        drawParagraph("Clinical Liability Disclaimer:", isBold = true, size = 9f, colorHex = "#2563EB")
        drawParagraph(
            "Dr SAM is an AI assistant, not a licensed medical practitioner. Compiled summaries are for therapeutic reference and general awareness. Seek immediate physical care for emergencies.", 
            isItalic = true, 
            size = 8f, 
            colorHex = "#1E40AF"
        )
        currentY += 12f

        // Divider
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
        currentY += 12f

        // 4. Render Chief Problems
        drawParagraph("I. ISOLATED HEALTH PROBLEMS & CHIEF COMPLAINTS", isBold = true, size = 11f, colorHex = "#2563EB")
        currentY += 6f

        diagnosis.chiefComplaints.forEach { complaint ->
            val formatted = "• $complaint"
            drawParagraph(formatted, size = 9.5f, colorHex = "#1E293B")
        }
        currentY += 12f

        // Divider
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
        currentY += 12f

        // 5. Render Recommendations & Solutions
        drawParagraph("II. DR SAM'S RECOMMENDATIONS & CLINICAL DIRECTIONS", isBold = true, size = 11f, colorHex = "#2563EB")
        currentY += 6f

        diagnosis.suggestionsList.forEach { solution ->
            drawParagraph(solution, size = 9.5f, colorHex = "#334155")
        }

        // Final footer check
        if (currentY + 25f > pageHeight - 50f) {
            addPage()
        }
        currentY += 10f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
        currentY += 8f
        drawParagraph("End of compiled medical diagnostic export. Seek formal healthcare provider review.", isItalic = true, size = 8.5f, colorHex = "#64748B")

        // Finalize document
        pdfDocument.finishPage(currentPage)

        try {
            val fileName = "Dr_SAM_Report_${System.currentTimeMillis()}.pdf"
            val file = File(getReportsDirectory(context), fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            shareFile(context, file, "application/pdf", "Dr SAM Health Report - $chatTitle")
            Log.d(TAG, "PDF Health report compiled successfully!")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to compile PDF report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
