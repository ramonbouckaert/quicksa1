package io.bouckaert.quicksa1.shared

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

data class PDFRendererDeps(
    val srid: Int = 3857,
    val geometryFactory: GeometryFactory = GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), srid)
)
