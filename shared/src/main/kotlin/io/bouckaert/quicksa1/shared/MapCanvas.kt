package io.bouckaert.quicksa1.shared

import io.bouckaert.quicksa1.shared.PolygonMapper.mapToJts
import kotlinx.coroutines.*
import net.postgis.jdbc.geometry.MultiPolygon
import org.geotools.data.DataUtilities
import org.geotools.data.memory.MemoryFeatureCollection
import org.geotools.factory.CommonFactoryFinder
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.map.FeatureLayer
import org.geotools.map.Layer
import org.geotools.map.MapContent
import org.geotools.styling.Style
import org.opengis.feature.simple.SimpleFeature
import java.awt.Color

abstract class MapCanvas(
    val sa1: MultiPolygon,
    coroutineScope: CoroutineScope,
    val log: (String) -> Unit
) {
    private val styleFactory = CommonFactoryFinder.getStyleFactory(null)
    private val filterFactory = CommonFactoryFinder.getFilterFactory(null)
    private val featureType = coroutineScope.async(start = CoroutineStart.LAZY) {
        DataUtilities.createType(
            "Boundary",
            "the_geom:MultiPolygon:srid=3857"
        )
    }
    private val invertedFeature: Deferred<SimpleFeature> = coroutineScope.async(start = CoroutineStart.LAZY) { createInvertedFeature() }

    val sa1Feature: Deferred<SimpleFeature> = coroutineScope.async(start = CoroutineStart.LAZY) { createSA1Feature() }
    val map: Deferred<MapContent> = coroutineScope.async(start = CoroutineStart.LAZY) { createMap() }

    abstract fun createUnderlayLayer(): Layer

    private suspend fun createMap(): MapContent {
        log("Instantiating map and adding layers")
        val featureCollection = MemoryFeatureCollection(featureType.await()).apply {
            add(invertedFeature.await())
        }

        return MapContent().apply {
            title = sa1.toString()
            addLayer(createUnderlayLayer())
            addLayer(
                FeatureLayer(
                    featureCollection,
                    createOutlineStyle()
                )
            )
        }
    }

    private suspend fun createSA1Feature(): SimpleFeature {
        log("Mapping SA1 geometry to JTS polygon")
        val multiPolygon = JTSFactoryFinder.getGeometryFactory().let {
            it.createMultiPolygon(
                sa1.polygons.map { geom -> geom.mapToJts(it, false) }.toTypedArray()
            )
        }

        return SimpleFeatureBuilder(featureType.await()).apply {
            add(multiPolygon)
        }.buildFeature(null)
    }

    private suspend fun createInvertedFeature(): SimpleFeature {
        log("Mapping SA1 geometry to inverted JTS polygon")
        val invertedMultiPolygon = JTSFactoryFinder.getGeometryFactory().let {
            it.createMultiPolygon(
                sa1.polygons.map { geom -> geom.mapToJts(it, true) }.toTypedArray()
            )
        }
        return SimpleFeatureBuilder(featureType.await()).apply {
            add(invertedMultiPolygon)
        }.buildFeature(null)
    }

    private fun createOutlineStyle(): Style {
        val rule = styleFactory.createRule()
        val stroke = styleFactory.createStroke(filterFactory.literal(Color.BLACK), filterFactory.literal(3.0f))
        val fill = styleFactory.createFill(filterFactory.literal(Color.BLACK), filterFactory.literal(0.3))
        val sym = styleFactory.createPolygonSymbolizer(stroke, fill, null)
        rule.symbolizers().add(sym)

        val fts = styleFactory.createFeatureTypeStyle(rule)
        val style = styleFactory.createStyle()
        style.featureTypeStyles().add(fts)

        return style
    }
}