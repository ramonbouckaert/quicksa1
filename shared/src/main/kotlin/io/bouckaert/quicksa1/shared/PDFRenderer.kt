package io.bouckaert.quicksa1.shared

import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.pdf.PdfGraphics2D
import com.lowagie.text.pdf.PdfWriter
import de.topobyte.chromaticity.ColorCode
import de.topobyte.jts.drawing.DrawMode
import de.topobyte.jts.drawing.awt.GeometryDrawerGraphics
import io.bouckaert.quicksa1.db.PolygonColumnType
import io.bouckaert.quicksa1.db.ST_MakeEnvelope
import io.bouckaert.quicksa1.db.within
import io.bouckaert.quicksa1.shared.PolygonMapper.mapToJts
import io.bouckaert.quicksa1.shared.db.entities.SA1
import io.bouckaert.quicksa1.shared.db.enums.BlockType
import io.bouckaert.quicksa1.shared.db.selectWith
import io.bouckaert.quicksa1.shared.db.tables.Blocks
import io.bouckaert.quicksa1.shared.db.tables.MultiPolygons
import io.bouckaert.quicksa1.shared.db.tables.Roads
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import net.postgis.jdbc.PGbox2d
import net.postgis.jdbc.geometry.Point
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.*
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.operation.linemerge.LineMerger
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.io.ByteArrayOutputStream

class PDFRenderer(
    private val database: Database,
    private val log: (log: String) -> Unit
) {
    private val srid = 3857
    private val geometryFactory = GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), srid)
    suspend fun renderPdf(sa1: Long): ByteArrayOutputStream? = coroutineScope {
        log("Fetching SA1 from database")
        val (sa1DatabaseGeometry, suburbName, suburbIndex) = fetchSA1(sa1) ?: return@coroutineScope null

        val sa1Geometry = sa1DatabaseGeometry.mapToJts(geometryFactory)
        val sa1InverseGeometry = sa1DatabaseGeometry.mapToJts(geometryFactory, true)

        val bounds = sa1Geometry.envelopeInternal
        val boundsAspectRatio = bounds.width / bounds.height
        val pageArea = PageSize.A4.let { if (boundsAspectRatio < 1.0) it else it.rotate() }

        log("Calculating page bounds")
        val pageAspectBounds = bounds.let { itBounds ->
            val pageAspectRatio = (pageArea.width / pageArea.height).toDouble()
            val width = itBounds.width.let {
                if (boundsAspectRatio > pageAspectRatio) it else it * (pageAspectRatio / boundsAspectRatio)
            }
            val height = itBounds.height.let {
                if (boundsAspectRatio < pageAspectRatio) it else it * (boundsAspectRatio / pageAspectRatio)
            }
            val xMid = (itBounds.maxX + itBounds.minX) / 2
            val yMid = (itBounds.maxY + itBounds.minY) / 2
            Envelope(
                xMid - (width / 2.0),
                xMid + (width / 2.0),
                yMid - (height / 2.0),
                yMid + (height / 2.0)
            )
        }

        val blocksDeferred = fetchBlocks(pageAspectBounds)

        val roadsDeferred = fetchRoads(pageAspectBounds)

        log("Instantiating PDF")
        val pdf = Document(pageArea, 0F, 0F, 0F, 0F)

        return@coroutineScope ByteArrayOutputStream().let { bos ->
            val pdfWriter = PdfWriter.getInstance(pdf, bos)
            pdf.open()

            log("Rendering graphics into PDF")
            val graphics2D = PdfGraphics2D(
                pdfWriter.directContent,
                pageArea.width,
                pageArea.height
            )

            val borderWidth = 20
            val bottomSpace = 30

            val pageCoordinateTransformer = PageCoordinateTransformer(
                pageArea.width.toDouble(),
                pageArea.height.toDouble(),
                PageCoordinateTransformer.PageBorder(
                    borderWidth.toDouble() + 10.0,
                    borderWidth.toDouble() + 10.0,
                    (borderWidth + bottomSpace).toDouble() + 10.0,
                    borderWidth.toDouble() + 10.0
                ),
                bounds.minX,
                bounds.width,
                bounds.minY,
                bounds.height
            )

            // Instantiate renderer
            val renderer = object : GeometryDrawerGraphics(
                pageCoordinateTransformer,
                (pageArea.width).toInt(),
                (pageArea.height).toInt()
            ) {
                override fun getGraphics(): Graphics2D = graphics2D
            }

            suspendedTransactionAsync(db = database) {
                // Sum geometry for roads of the same name
                val collectedRoads =
                    roadsDeferred.fold(mutableMapOf<String, RoadDto>()) { acc, roadDto ->
                        if (roadDto.name != null) {
                            val existing = acc[roadDto.name]
                            if (
                                existing == null
                            ) {
                                acc[roadDto.name] = roadDto
                            } else {
                                acc[roadDto.name] = RoadDto(
                                    roadDto.name,
                                    existing.geometry.safeUnion(roadDto.geometry, geometryFactory)
                                )
                            }
                        }
                        acc
                    }


                // Draw roads geometry
                renderer.setColorBackground(ColorCode(180, 180, 180))
                collectedRoads.forEach { road ->
                    for (i in 0 until road.value.geometry.numGeometries) {
                        renderer.drawGeometry(road.value.geometry.getGeometryN(i), DrawMode.FILL)
                    }
                }

                // Draw blocks geometry
                renderer.setColorForeground(ColorCode(0, 0, 0))
                graphics2D.stroke = BasicStroke(0.2F)
                blocksDeferred.collect { block ->
                    when (block.type) {
                        BlockType.RESIDENTIAL -> renderer.setColorBackground(ColorCode(255, 255, 255))
                        BlockType.COMMERCIAL -> renderer.setColorBackground(ColorCode(200, 200, 200))
                        BlockType.COMMUNITY -> renderer.setColorBackground(ColorCode(255, 215, 145))
                        BlockType.PARK -> renderer.setColorBackground(ColorCode(122, 171, 113))
                        BlockType.WATER -> renderer.setColorBackground(ColorCode(113, 141, 171))
                    }
                    for (i in 0 until block.geometry.numGeometries) {
                        renderer.drawGeometry(block.geometry.getGeometryN(i), DrawMode.FILL_OUTLINE)
                    }
                    if (block.streetNumber != null) {
                        val centroid = block.geometry.centroid
                        val text = block.streetNumber.toString()
                        renderer.drawString(centroid.x, centroid.y, text, 5, -text.length, 0)
                    }
                }

                // Draw road labels
                collectedRoads.map { road ->
                    val voronoi = try {
                        VoronoiDiagramBuilder().apply {
                            setSites(
                                DouglasPeuckerSimplifier(road.value.geometry.union())
                                    .apply { setDistanceTolerance(2.0) }
                                    .resultGeometry
                            )
                            setClipEnvelope(road.value.geometry.envelopeInternal)
                        }.getDiagram(geometryFactory)
                    } catch (e: TopologyException) {
                        null
                    }

                    val centrelineMerger = LineMerger()
                    var lineAdded = false
                    if (voronoi != null) {
                        for (i in 0 until voronoi.numGeometries) {
                            val geom = voronoi.getGeometryN(i).boundary
                            for (j in 0 until geom.numPoints - 1) {
                                val actualGeom: LineString =
                                    if (geom is MultiLineString) geom.getGeometryN(0) as LineString else geom as LineString
                                val individualLine = try {
                                    LineString(
                                        CoordinateArraySequence(
                                            arrayOf(
                                                actualGeom.getCoordinateN(j),
                                                actualGeom.getCoordinateN(j + 1)
                                            )
                                        ),
                                        geometryFactory
                                    )
                                } catch (e: IndexOutOfBoundsException) {
                                    null
                                }
                                if (
                                    individualLine != null &&
                                    individualLine.safeWithin(road.value.geometry)
                                ) {
                                    centrelineMerger.add(individualLine)
                                    lineAdded = true
                                }
                            }
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    val centreLineStrings: Collection<LineString> = if (lineAdded) {
                        centrelineMerger.mergedLineStrings as Collection<LineString>
                    } else {
                        var geomArray: Array<LineString> = emptyArray()
                        when (road.value.geometry) {
                            is LineString -> geomArray += road.value.geometry as LineString
                            is Polygon -> geomArray += (road.value.geometry as Polygon).exteriorRing
                            is MultiPolygon, is GeometryCollection -> {
                                for (i in 0 until road.value.geometry.numGeometries) {
                                    when (val innerGeom = road.value.geometry.getGeometryN(i)) {
                                        is LineString -> geomArray += innerGeom
                                        is Polygon -> geomArray += innerGeom.exteriorRing
                                    }
                                }
                            }
                        }
                        geomArray.toList()
                    }.map {
                        DouglasPeuckerSimplifier(it)
                            .apply { setDistanceTolerance(20.0) }
                            .resultGeometry as LineString
                    }
                    val longest = centreLineStrings.fold(null as LineString?) { acc, lineString: LineString ->
                        if (acc == null) lineString else {
                            if (acc.length > lineString.length) acc else lineString
                        }
                    }.apply { this?.normalize() }
                    if (longest != null) {
                        val origRotate = graphics2D.transform
                        graphics2D.translate(
                            pageCoordinateTransformer.getX(longest.centroid.x),
                            pageCoordinateTransformer.getY(longest.centroid.y)
                        )
                        var angle = Angle.normalizePositive(
                            Angle.angle(
                                longest.getCoordinateN(0),
                                longest.getCoordinateN(1)
                            )
                        )
                        while (angle > (Math.PI / 2)) angle -= Math.PI
                        graphics2D.rotate(-angle)
                        graphics2D.font = Font("Verdana", Font.PLAIN, 6)
                        graphics2D.drawString(road.key.titlecase(), -road.key.length * 2, 0)
                        graphics2D.transform = origRotate
                    }
                }
            }.await()

            // Draw SA1 geometry
            graphics2D.stroke = BasicStroke(5F)
            renderer.setColorBackground(ColorCode(0.0F, 0.0F, 0.0F, 0.5F))
            for (i in 0 until sa1InverseGeometry.numGeometries) {
                renderer.drawGeometry(sa1InverseGeometry.getGeometryN(i), DrawMode.FILL_OUTLINE)
            }

            // Draw border
            graphics2D.paint = Color.GRAY
            graphics2D.fillRect(0, 0, pageArea.width.toInt(), borderWidth)
            graphics2D.fillRect(0, 0, borderWidth, pageArea.height.toInt())
            graphics2D.fillRect(pageArea.width.toInt() - borderWidth, 0, borderWidth, pageArea.height.toInt())
            graphics2D.fillRect(
                0,
                pageArea.height.toInt() - (borderWidth + bottomSpace),
                pageArea.width.toInt(),
                (borderWidth + bottomSpace)
            )

            // Add name of SA1 to bottom
            graphics2D.paint = Color.WHITE
            graphics2D.font = Font("Verdana", Font.BOLD, 20)
            graphics2D.drawString(
                "$sa1 - $suburbName ${suburbIndex + 1}",
                borderWidth + 10,
                pageArea.height.toInt() - borderWidth
            )

            // Add my name to the bottom-right
            val signature1 = "QuickSA1 was written by Ramon Bouckaert"
            val signature2 = "www.quicksa1.com"
            graphics2D.font = Font("Verdana", Font.PLAIN, 6)
            graphics2D.drawString(
                signature1,
                pageArea.width.toInt() - (borderWidth + graphics2D.fontMetrics.stringWidth(signature1)),
                pageArea.height.toInt() - (borderWidth + graphics2D.fontMetrics.height)
            )
            graphics2D.drawString(
                signature2,
                pageArea.width.toInt() - (borderWidth + graphics2D.fontMetrics.stringWidth(signature2)),
                pageArea.height.toInt() - borderWidth
            )

            graphics2D.dispose()

            pdf.close()
            pdfWriter.close()

            bos
        }
    }

    private suspend fun fetchSA1(id: Long): Triple<net.postgis.jdbc.geometry.MultiPolygon, String, Int>? =
        suspendedTransactionAsync(db = database) {
            SA1.findById(id)?.let {
                Triple(
                    it.polygon.geometry,
                    it.sa2.name,
                    it.sa2.sa1s.sortedBy { sa1 -> sa1.id }.indexOf(it)
                )
            }
        }.await()

    private fun fetchBlocks(bounds: Envelope): Flow<BlockDto> =
        Blocks.innerJoin(MultiPolygons).selectWith(
            ST_MakeEnvelope(
                PGbox2d(
                    Point(bounds.minX, bounds.maxY),
                    Point(bounds.maxX, bounds.minY)
                )
            )
        ) {
            MultiPolygons.geometry.within(
                LiteralOp(PolygonColumnType(), "w.val")
            )
        }.asFlow().map {
            BlockDto(
                it[Blocks.streetNumber],
                it[Blocks.type],
                it[MultiPolygons.geometry].mapToJts(geometryFactory)
            )
        }

    private fun fetchRoads(bounds: Envelope): Flow<RoadDto> =
        Roads.innerJoin(MultiPolygons).selectWith(
            ST_MakeEnvelope(
                PGbox2d(
                    Point(bounds.minX, bounds.maxY),
                    Point(bounds.maxX, bounds.minY)
                )
            )
        ) {
            MultiPolygons.geometry.within(
                LiteralOp(PolygonColumnType(), "w.val")
            )
        }.asFlow().map {
            RoadDto(
                it[Roads.name],
                it[MultiPolygons.geometry].mapToJts(geometryFactory)
            )
        }

    private data class BlockDto(
        val streetNumber: Int?,
        val type: BlockType,
        val geometry: Geometry
    )

    private data class RoadDto(
        val name: String?,
        val geometry: Geometry
    )

    private fun String.titlecase() = this
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

    private inline fun String.replaceFirstChar(transform: (Char) -> CharSequence): String =
        if (isNotEmpty()) transform(this[0]).toString() + substring(1) else this
}