package com.moneymate.lite.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CollectionSheetRow(
    val customerName: String,
    val balance: Double
)

object CollectionSheetPdfGenerator {

    // US Legal paper size in points at 72 DPI (8.5 in x 14.0 in)
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 1008

    private const val MARGIN_X = 20f
    private const val MARGIN_TOP = 28f
    private const val MARGIN_BOTTOM = 22f
    private const val TABLE_GAP = 16f

    // Total usable width = 612 - 40 = 572
    // Each table width = (572 - 16) / 2 = 278
    private const val TABLE_WIDTH = 278f
    private const val NAME_COL_WIDTH = 88f
    private const val BALANCE_COL_WIDTH = 46f
    // 7 empty writing cells for collections: 144 / 7 = ~20.57f each
    private const val EMPTY_CELL_WIDTH = 20.57f
    private const val EMPTY_CELLS_COUNT = 7

    private const val HEADER_ROW_HEIGHT = 22f
    private const val DATA_ROW_HEIGHT = 24f

    // Available height for rows: 1008 - 28 - 22 - 38 (header) - 22 (table header) = 898
    // 898 / 24 = ~37 rows per column, 74 per page
    private const val ROWS_PER_COLUMN = 36
    private const val ITEMS_PER_PAGE = ROWS_PER_COLUMN * 2 // 72 per page

    fun generatePdf(
        context: Context,
        fileName: String,
        dateMillis: Long,
        rows: List<CollectionSheetRow>
    ): File {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(dateMillis))

        val pdfDocument = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val balancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 70, 70)
            textSize = 8.5f
            typeface = Typeface.DEFAULT
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(238, 238, 238)
            style = Paint.Style.FILL
        }

        val totalBalance = rows.sumOf { it.balance }
        val totalPages = maxOf(1, (rows.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)

        for (pageIndex in 0 until totalPages) {
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Page Header Top (y = 28 to 60)
            var currentY = MARGIN_TOP

            // Title Line
            canvas.drawText("File: $fileName", MARGIN_X, currentY + 12f, titlePaint)
            val centerTitle = "DAILY COLLECTION SHEET"
            val centerTitleWidth = titlePaint.measureText(centerTitle)
            canvas.drawText(centerTitle, (PAGE_WIDTH - centerTitleWidth) / 2f, currentY + 12f, titlePaint)
            val dateText = "Date: $dateStr"
            val dateWidth = titlePaint.measureText(dateText)
            canvas.drawText(dateText, PAGE_WIDTH - MARGIN_X - dateWidth, currentY + 12f, titlePaint)

            // Subtitle Line
            val summaryText = "Active Customers: ${rows.size}   |   Total Outstanding Balance: ₹${"%,.0f".format(totalBalance)}"
            canvas.drawText(summaryText, MARGIN_X, currentY + 26f, metaPaint)
            val pageStr = "Page $pageNumber of $totalPages (Legal Sheet)"
            val pageStrWidth = metaPaint.measureText(pageStr)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X - pageStrWidth, currentY + 26f, metaPaint)

            currentY += 34f

            // Slice rows for this page
            val startIdx = pageIndex * ITEMS_PER_PAGE
            val endIdx = minOf(rows.size, startIdx + ITEMS_PER_PAGE)
            val pageRows = if (startIdx < rows.size) rows.subList(startIdx, endIdx) else emptyList()

            // Balance items between Left and Right columns
            val half = (pageRows.size + 1) / 2
            val leftColRows = pageRows.take(half)
            val rightColRows = pageRows.drop(half)

            val leftTableStartX = MARGIN_X
            val rightTableStartX = MARGIN_X + TABLE_WIDTH + TABLE_GAP

            // Draw Left Column Table
            drawColumnTable(
                canvas = canvas,
                startX = leftTableStartX,
                startY = currentY,
                rows = leftColRows,
                textPaint = textPaint,
                balancePaint = balancePaint,
                headerTextPaint = headerTextPaint,
                headerBgPaint = headerBgPaint,
                borderPaint = borderPaint
            )

            // Draw Right Column Table
            drawColumnTable(
                canvas = canvas,
                startX = rightTableStartX,
                startY = currentY,
                rows = rightColRows,
                textPaint = textPaint,
                balancePaint = balancePaint,
                headerTextPaint = headerTextPaint,
                headerBgPaint = headerBgPaint,
                borderPaint = borderPaint
            )

            pdfDocument.finishPage(page)
        }

        // Save PDF to cache directory
        val outputDir = File(context.cacheDir, "collection_sheets").apply { mkdirs() }
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val outFile = File(outputDir, "Collection_Sheet_${safeFileName}_${dateStr}.pdf")
        val outStream = FileOutputStream(outFile)
        pdfDocument.writeTo(outStream)
        outStream.flush()
        outStream.close()
        pdfDocument.close()

        return outFile
    }

    private fun drawColumnTable(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        rows: List<CollectionSheetRow>,
        textPaint: Paint,
        balancePaint: Paint,
        headerTextPaint: Paint,
        headerBgPaint: Paint,
        borderPaint: Paint
    ) {
        val xName = startX
        val xBalance = xName + NAME_COL_WIDTH
        val xBoxes = FloatArray(EMPTY_CELLS_COUNT + 1)
        xBoxes[0] = xBalance + BALANCE_COL_WIDTH
        for (i in 1..EMPTY_CELLS_COUNT) {
            xBoxes[i] = xBoxes[i - 1] + EMPTY_CELL_WIDTH
        }
        val endX = startX + TABLE_WIDTH

        // Table Header Row
        var y = startY
        canvas.drawRect(startX, y, endX, y + HEADER_ROW_HEIGHT, headerBgPaint)
        canvas.drawRect(startX, y, endX, y + HEADER_ROW_HEIGHT, borderPaint)

        // Vertical dividers in header
        canvas.drawLine(xBalance, y, xBalance, y + HEADER_ROW_HEIGHT, borderPaint)
        for (i in 0 until EMPTY_CELLS_COUNT) {
            canvas.drawLine(xBoxes[i], y, xBoxes[i], y + HEADER_ROW_HEIGHT, borderPaint)
        }

        // Header text
        val nameHeaderY = y + HEADER_ROW_HEIGHT / 2f + 3f
        canvas.drawText("Customer Name", xName + 4f, nameHeaderY, headerTextPaint)
        canvas.drawText("Balance", xBalance + 4f, nameHeaderY, headerTextPaint)

        y += HEADER_ROW_HEIGHT

        // Data Rows
        val rowCount = maxOf(rows.size, if (rows.isNotEmpty()) ROWS_PER_COLUMN else 0)
        for (rowIndex in 0 until rowCount) {
            val row = rows.getOrNull(rowIndex)
            val rowTop = y
            val rowBottom = y + DATA_ROW_HEIGHT

            // Row outer rect
            canvas.drawRect(startX, rowTop, endX, rowBottom, borderPaint)

            // Vertical column dividers
            canvas.drawLine(xBalance, rowTop, xBalance, rowBottom, borderPaint)
            for (i in 0 until EMPTY_CELLS_COUNT) {
                canvas.drawLine(xBoxes[i], rowTop, xBoxes[i], rowBottom, borderPaint)
            }

            if (row != null) {
                val textBaseline = rowTop + DATA_ROW_HEIGHT / 2f + 3.5f

                // Formatted customer name with trailing '-' as requested in Word sample
                val formattedName = if (row.customerName.endsWith("-")) row.customerName else "${row.customerName}-"
                val availableNameWidth = NAME_COL_WIDTH - 6f
                val ellipsizedName = TextUtils.ellipsize(
                    formattedName,
                    TextPaint(textPaint),
                    availableNameWidth,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(ellipsizedName, xName + 3f, textBaseline, textPaint)

                // Formatted balance in first box next to name
                val balanceText = "₹${"%.0f".format(row.balance)}"
                val availableBalanceWidth = BALANCE_COL_WIDTH - 6f
                val ellipsizedBalance = TextUtils.ellipsize(
                    balanceText,
                    TextPaint(balancePaint),
                    availableBalanceWidth,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(ellipsizedBalance, xBalance + 3f, textBaseline, balancePaint)

                // Boxes 1 through 7 remain empty for manual pencil/pen collection entry!
            }

            y += DATA_ROW_HEIGHT
        }
    }

    fun shareOrOpenPdf(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Open or Print Collection Sheet").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return shareOrOpenPdf(context, pdfFile, jobName)

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.NA_LEGAL)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }
}
