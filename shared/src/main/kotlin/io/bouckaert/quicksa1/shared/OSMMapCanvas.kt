package io.bouckaert.quicksa1.shared

import kotlinx.coroutines.CoroutineScope
import net.postgis.jdbc.geometry.MultiPolygon
import org.geotools.map.Layer
import org.geotools.tile.impl.osm.OSMService
import org.geotools.tile.util.TileLayer

class OSMMapCanvas(
    sa1: MultiPolygon,
    coroutineScope: CoroutineScope,
    log: (String) -> Unit
) : MapCanvas(sa1, coroutineScope, log) {
    override fun createUnderlayLayer(): Layer {
        super.log("Instantiating underlay service")
        return TileLayer(
            OSMService(
                "OSM",
                "http://tile.openstreetmap.org/"
            )
        )
    }
}