package io.bouckaert.quicksa1.shared

import io.bouckaert.quicksa1.db.ST_IntersectsEnvelope
import io.bouckaert.quicksa1.shared.PolygonMapper.mapToJts
import io.bouckaert.quicksa1.shared.db.entities.Block
import io.bouckaert.quicksa1.shared.db.entities.Road
import io.bouckaert.quicksa1.shared.db.tables.Blocks
import io.bouckaert.quicksa1.shared.db.tables.MultiPolygons
import io.bouckaert.quicksa1.shared.db.tables.Roads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import net.postgis.jdbc.PGbox2d
import net.postgis.jdbc.geometry.MultiPolygon
import net.postgis.jdbc.geometry.Point
import org.geotools.data.DataUtilities
import org.geotools.data.memory.MemoryFeatureCollection
import org.geotools.factory.CommonFactoryFinder
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.map.FeatureLayer
import org.geotools.map.Layer
import org.geotools.styling.Fill
import org.geotools.styling.Stroke
import org.geotools.styling.Style
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.opengis.feature.simple.SimpleFeature
import org.opengis.geometry.BoundingBox
import java.awt.Color

class CadastreMapCanvas(
    val database: Database,
    sa1: MultiPolygon,
    coroutineScope: CoroutineScope,
    log: (String) -> Unit
) : MapCanvas(sa1, coroutineScope, log) {

    private val geometryFactory = JTSFactoryFinder.getGeometryFactory()
    private val styleFactory = CommonFactoryFinder.getStyleFactory(null)
    private val filterFactory = CommonFactoryFinder.getFilterFactory(null)

    private val featureType = coroutineScope.async(start = CoroutineStart.LAZY) {
        DataUtilities.createType(
            "Block",
            "blocks:MultiPolygon:srid=3857"
        )
    }

    override suspend fun createUnderlayLayers(boundingBox: BoundingBox): List<Layer> {
        super.log("Instantiating underlay service")
        val bounds = ReferencedEnvelope(sa1Feature.await().bounds).apply {
            expandBy((this.width * 0.15) / 2, (this.height * 0.15) / 2)
        }

        val roadsFeatureCollection = MemoryFeatureCollection(featureType.await()).apply {
            addAll(getRoads(bounds))
        }

        val blocksFeatureCollection = MemoryFeatureCollection(featureType.await()).apply {
            addAll(getBlocks(bounds))
        }

        return listOf(
            FeatureLayer(
                roadsFeatureCollection,
                createRoadStyle()
            ),
            FeatureLayer(
                blocksFeatureCollection,
                createBlockStyle()
            )
        )
    }

    private suspend fun getBlocks(boundingBox: BoundingBox): List<SimpleFeature> {
        return suspendedTransactionAsync(db = database) {
            val blocks = Blocks.leftJoin(MultiPolygons).select {
                ST_IntersectsEnvelope(MultiPolygons.geometry, boundingBox.toPGBox2d()) eq true
            }.toList().map { Block.wrapRow(it) }

            blocks.map { block ->
                geometryFactory.createMultiPolygon(
                    block.polygon.geometry.polygons.map { polygon -> polygon.mapToJts(geometryFactory, false) }
                        .toTypedArray()
                )
            }
        }.await()
            .map { geom ->
                SimpleFeatureBuilder(featureType.await()).apply {
                    add(geom)
                }.buildFeature(null)
            }
    }

    private suspend fun getRoads(boundingBox: BoundingBox): List<SimpleFeature> {
        return suspendedTransactionAsync(db = database) {
            val roads = Roads.leftJoin(MultiPolygons).select {
                ST_IntersectsEnvelope(MultiPolygons.geometry, boundingBox.toPGBox2d()) eq true
            }.toList().map { Road.wrapRow(it) }

            roads.map { road ->
                geometryFactory.createMultiPolygon(
                    road.polygon.geometry.polygons.map { polygon -> polygon.mapToJts(geometryFactory, false) }
                        .toTypedArray()
                )
            }
        }.await()
            .map { geom ->
                SimpleFeatureBuilder(featureType.await()).apply {
                    add(geom)
                }.buildFeature(null)
            }
    }

    private fun createBlockStyle(): Style {
        val rule = styleFactory.createRule()
        val stroke = styleFactory.createStroke(filterFactory.literal(Color.BLACK), filterFactory.literal(3.0f))
        val sym = styleFactory.createPolygonSymbolizer(stroke, Fill.NULL, null)
        rule.symbolizers().add(sym)

        val fts = styleFactory.createFeatureTypeStyle(rule)
        val style = styleFactory.createStyle()
        style.featureTypeStyles().add(fts)

        return style
    }

    private fun createRoadStyle(): Style {
        val rule = styleFactory.createRule()
        val fill = styleFactory.createFill(filterFactory.literal(Color.GRAY))
        val sym = styleFactory.createPolygonSymbolizer(Stroke.NULL, fill, null)
        rule.symbolizers().add(sym)

        val fts = styleFactory.createFeatureTypeStyle(rule)
        val style = styleFactory.createStyle()
        style.featureTypeStyles().add(fts)

        return style
    }

    private fun BoundingBox.toPGBox2d(): PGbox2d = PGbox2d(Point(this.minX, this.minY), Point(this.maxX, this.maxY))
}