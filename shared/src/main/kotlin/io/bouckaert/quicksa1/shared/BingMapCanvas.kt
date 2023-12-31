package io.bouckaert.quicksa1.shared

import kotlinx.coroutines.CoroutineScope
import net.postgis.jdbc.geometry.MultiPolygon
import org.geotools.map.Layer
import org.geotools.tile.impl.bing.BingService
import org.geotools.tile.util.TileLayer
import org.opengis.geometry.BoundingBox

class BingMapCanvas(
    sa1: MultiPolygon,
    coroutineScope: CoroutineScope,
    log: (String) -> Unit
) : MapCanvas(sa1, coroutineScope, log) {
    override suspend fun createUnderlayLayers(boundingBox: BoundingBox): List<Layer> {
        super.log("Instantiating underlay service")
        return listOf(
            TileLayer(
                BingService(
                    "Bing",
                    "http://ecn.t1.tiles.virtualearth.net/tiles/r\${code}.jpeg?key=AgjMlzAq-pgREkh85xHgcV_XaXWV5FEScxoxN1eRN8ERNKwc1n3xooobR6NCAYIA&g=129&mkt={culture}&shading=hill&stl=H;"
                )
            )
        )
    }
}