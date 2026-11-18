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
    private const val MARGIN_TOP = 20f
    private const val MARGIN_BOTTOM = 20f
    private const val TABLE_GAP = 14f

    // Total usable width = 612 - 40 = 572
    // Each table width = (572 - 14) / 2 = 279
    private const val TABLE_WIDTH = 279f
    private const val NAME_COL_WIDTH = 70f // Reduced space for customer name to eliminate large gap
    private const val BALANCE_COL_WIDTH = 38f // Small box for amount (cleanly fits amounts up to 6 digits)
    // 7 empty writing cells for collections: (279 - 70 - 38) / 7 = 171 / 7 = ~24.43f each (expanded boxes!)
    private const val EMPTY_CELL_WIDTH = 24.43f
    private const val EMPTY_CELLS_COUNT = 7

    private const val DATA_ROW_HEIGHT = 24.2f

    // Available height for rows: 1008 - 20 - 20 = 968
    // 968 / 24.2 = 40 rows per column, 80 rows per page
    private const val ROWS_PER_COLUMN = 40
    private const val CUSTOMERS_PER_BLOCK = 8
    // 4 blocks of (8 customers + 1 empty separator row) + 4 customers = 36 customers + 4 empty rows = 40 rows
    private const val CUSTOMERS_PER_COLUMN = 36
    private const val ITEMS_PER_PAGE = CUSTOMERS_PER_COLUMN * 2 // 72 customers per Legal page

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

        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val totalPages = maxOf(1, (rows.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)

        for (pageIndex in 0 until totalPages) {
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // No heading and no date: tables start directly from top margin
            val currentY = MARGIN_TOP

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

        var y = startY

        // Build 40 rows with an empty separator row after every 8 customer rows
        val tableRows = buildTableRows(rows)

        for (row in tableRows) {
            val rowTop = y
            val rowBottom = y + DATA_ROW_HEIGHT

            // Row outer rect
            canvas.drawRect(startX, rowTop, endX, rowBottom, borderPaint)

            // Vertical column dividers
            canvas.drawLine(xBalance, rowTop, xBalance, rowBottom, borderPaint)
            for (i in 0 until EMPTY_CELLS_COUNT) {
                canvas.drawLine(xBoxes[i], rowTop, xBoxes[i], rowBottom, borderPaint)
            }

            if (row != null && row.customerName.isNotBlank()) {
                val textBaseline = rowTop + DATA_ROW_HEIGHT / 2f + 3.5f

                // Formatted customer name with trailing '-' as in Word template
                val formattedName = if (row.customerName.endsWith("-")) row.customerName else "${row.customerName}-"
                val availableNameWidth = NAME_COL_WIDTH - 6f
                val ellipsizedName = TextUtils.ellipsize(
                    formattedName,
                    TextPaint(textPaint),
                    availableNameWidth,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(ellipsizedName, xName + 3f, textBaseline, textPaint)

                // Formatted balance in amount column: NO rupee symbol!
                if (row.balance > 0) {
                    val balanceText = "%.0f".format(row.balance)
                    val textWidth = balancePaint.measureText(balanceText)
                    val textX = xBalance + (BALANCE_COL_WIDTH - textWidth) / 2f
                    canvas.drawText(balanceText, textX, textBaseline, balancePaint)
                }

                // Boxes 1 through 7 remain empty for manual pencil/pen collection entry!
            }

            y += DATA_ROW_HEIGHT
        }
    }

    private fun buildTableRows(customers: List<CollectionSheetRow>): List<CollectionSheetRow?> {
        val result = mutableListOf<CollectionSheetRow?>()
        var count = 0
        for (c in customers) {
            if (count > 0 && count % CUSTOMERS_PER_BLOCK == 0) {
                result.add(null) // Empty separator row after every 8 customer rows
            }
            result.add(c)
            count++
            if (result.size >= ROWS_PER_COLUMN) break
        }
        while (result.size < ROWS_PER_COLUMN) {
            result.add(null)
        }
        return result
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

    /**
     * Saves the generated PDF directly into the device's public Downloads folder
     * so the user can easily find, download, and open it from their phone's file manager.
     */
    fun downloadPdfToStorage(
        context: Context,
        pdfFile: File,
        displayName: String,
        dateMillis: Long
    ): Result<String> {
        return try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date(dateMillis))
            val cleanName = displayName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val fileName = "Collection_Sheet_${cleanName}_${dateStr}.pdf"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Could not create entry in Downloads"))

                resolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(pdfFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Result.success("Saved to Downloads/$fileName")
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                pdfFile.copyTo(targetFile, overwrite = true)
                Result.success("Saved to Downloads/$fileName")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
