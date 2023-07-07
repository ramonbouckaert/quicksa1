package io.bouckaert.quicksa1.shared

import kotlinx.coroutines.CoroutineScope
import net.postgis.jdbc.geometry.MultiPolygon
import org.geotools.map.Layer
import org.geotools.tile.impl.osm.OSMService
import org.geotools.tile.util.TileLayer
import org.opengis.geometry.BoundingBox

class OSMMapCanvas(
    sa1: MultiPolygon,
    coroutineScope: CoroutineScope,
    log: (String) -> Unit
) : MapCanvas(sa1, coroutineScope, log) {
    override suspend fun createUnderlayLayers(boundingBox: BoundingBox): List<Layer> {
        super.log("Instantiating underlay service")
        return listOf(
            TileLayer(
                OSMService(
                    "OSM",
                    "http://tile.openstreetmap.org/"
                )
            )
        )
    }
}