package io.bouckaert.quicksa1.shared

import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.pdf.PdfGraphics2D
import com.lowagie.text.pdf.PdfWriter
import io.bouckaert.quicksa1.shared.db.entities.SA1
import kotlinx.coroutines.CoroutineScope
import net.postgis.jdbc.geometry.MultiPolygon
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.renderer.lite.StreamingRenderer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.opengis.feature.simple.SimpleFeature
import java.awt.Color
import java.awt.Font
import java.awt.Rectangle
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext

class PDFRenderer(
    private val database: Database,
    private val log: (log: String) -> Unit
) {
    companion object {
        const val SCALE = 2
    }
    suspend fun renderPdf(sa1: Long): ByteArrayOutputStream? {
        val coroutineScope = CoroutineScope(coroutineContext)

        log("Fetching SA1 from database")
        val (geometry, suburbName, suburbIndex) = fetchSA1(sa1) ?: return null

        log("Instantiating map")
        val mapCanvas = CadastreMapCanvas(database, geometry, coroutineScope, log)
        val map = mapCanvas.map.await()

        log("Instantiating renderer")
        val renderer = StreamingRenderer().apply { mapContent = map }

        log("Calculating boundaries")
        val (pageArea, mapArea) = calculateBounds(mapCanvas.sa1Feature.await())
        val scaledPageArea = pageArea.scale(SCALE)

        log("Setting map to boundaries")
        map.viewport.screenArea = scaledPageArea
        map.viewport.bounds = mapArea

        log("Instantiating PDF")
        val pdf = Document(pageArea.toLowagie(), 0F, 0F, 0F, 0F)

        return ByteArrayOutputStream().let { bos ->
            val pdfWriter = PdfWriter.getInstance(pdf, bos)
            pdf.open()

            log("Rendering graphics into PDF")
            val graphics2D = PdfGraphics2D(
                pdfWriter.directContent,
                pageArea.width.toFloat(),
                pageArea.height.toFloat()
            )

            graphics2D.scale(1.0/SCALE, 1.0/SCALE)

            // Draw map on PDF
            renderer.paint(
                graphics2D,
                Rectangle(0, 0, scaledPageArea.width, scaledPageArea.height),
                mapArea
            )

            // Draw border
            val borderWidth = 20 * SCALE
            val bottomSpace = 30 * SCALE
            graphics2D.paint = Color.GRAY
            graphics2D.fillRect(0, 0, scaledPageArea.width, borderWidth)
            graphics2D.fillRect(0, 0, borderWidth, scaledPageArea.height)
            graphics2D.fillRect(scaledPageArea.width - borderWidth, 0, borderWidth, scaledPageArea.height)
            graphics2D.fillRect(
                0,
                scaledPageArea.height - (borderWidth + bottomSpace),
                scaledPageArea.width,
                (borderWidth + bottomSpace)
            )

            // Add name of SA1 to bottom
            graphics2D.paint = Color.WHITE
            graphics2D.font = Font("Arial", Font.BOLD, 20 * SCALE)
            graphics2D.drawString(
                "$sa1 - $suburbName ${suburbIndex + 1}",
                borderWidth + 10 * SCALE,
                scaledPageArea.height - borderWidth
            )

            graphics2D.dispose()

            pdf.close()
            pdfWriter.close()

            bos
        }
    }

    private suspend fun fetchSA1(id: Long): Triple<MultiPolygon, String, Int>? =
        suspendedTransactionAsync(db = database) {
            SA1.findById(id)?.let {
                Triple(
                    it.polygon.geometry,
                    it.sa2.name,
                    it.sa2.sa1s.sortedBy { sa1 -> sa1.id }.indexOf(it)
                )
            }
        }.await()

    private fun calculateBounds(feature: SimpleFeature): Bounds {
        val mapBounds = ReferencedEnvelope(feature.bounds)
        val heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0)
        val pageDimensions = PageSize.A4.let { if (heightToWidth > 1.0) it else it.rotate() }
        val pageHeightToWidth = pageDimensions.height / pageDimensions.width
        // Expand bounds to fill page
        if (heightToWidth > pageHeightToWidth) {
            mapBounds.expandBy(
                ((mapBounds.width * heightToWidth / pageHeightToWidth) - mapBounds.width) / 2,
                0.0
            )
        } else {
            mapBounds.expandBy(
                0.0,
                ((mapBounds.height * pageHeightToWidth / heightToWidth) - mapBounds.height) / 2
            )
        }
        // Add padding
        val paddingProportion = 0.15F
        mapBounds.expandBy(
            (mapBounds.width * paddingProportion) / 2,
            (mapBounds.height * paddingProportion) / 2
        )
        // Add extra padding on the bottom
        val bottomPaddingProportion = 0.02F
        mapBounds.expandBy(
            0.0,
            mapBounds.height * bottomPaddingProportion
        )
        mapBounds.translate(0.0, -(mapBounds.height * bottomPaddingProportion))

        return Bounds(pageDimensions.toAwt(), mapBounds)
    }

    private data class Bounds(
        val pageArea: Rectangle,
        val mapArea: ReferencedEnvelope
    )
}