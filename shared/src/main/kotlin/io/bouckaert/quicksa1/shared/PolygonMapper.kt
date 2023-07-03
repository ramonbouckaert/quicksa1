package io.bouckaert.quicksa1.shared

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.impl.CoordinateArraySequence

object PolygonMapper {
    @JvmStatic
    fun net.postgis.jdbc.geometry.Polygon.mapToJts(
        geometryFactory: GeometryFactory,
        invert: Boolean = false
    ): org.locationtech.jts.geom.Polygon {
        var innerRings: Array<net.postgis.jdbc.geometry.LinearRing> = emptyArray()

        val startInnerRing = if (invert) 0 else 1
        for (i in startInnerRing until numRings()) {
            innerRings += getRing(i)
        }

        val outerRing = if (invert) {
            this.getRing(0).boundingLinearRing(geometryFactory)
        } else {
            this.getRing(0).mapToJts(geometryFactory, true)
        }

        return org.locationtech.jts.geom.Polygon(
            outerRing,
            innerRings.map { it.mapToJts(geometryFactory, false) }.toTypedArray(),
            geometryFactory
        )
    }

    @JvmStatic
    fun net.postgis.jdbc.geometry.LinearRing.mapToJts(
        geometryFactory: GeometryFactory,
        invert: Boolean = false
    ): org.locationtech.jts.geom.LinearRing {
        return org.locationtech.jts.geom.LinearRing(
            CoordinateArraySequence(
                points
                    .map { it.mapToJts() }
                    .let { if (invert) it.reversed() else it }
                    .toTypedArray()
            ),
            geometryFactory
        )
    }

    @JvmStatic
    fun net.postgis.jdbc.geometry.Point.mapToJts() = org.locationtech.jts.geom.Coordinate(x, y, z)

    private fun net.postgis.jdbc.geometry.LinearRing.boundingLinearRing(
        geometryFactory: GeometryFactory,
    ): org.locationtech.jts.geom.LinearRing {
        return (this.mapToJts(geometryFactory).envelope as org.locationtech.jts.geom.Polygon)
            .exteriorRing.expandEnvelope(1_000_000F, geometryFactory)
    }

    private fun org.locationtech.jts.geom.LinearRing.expandEnvelope(
        number: Float,
        geometryFactory: GeometryFactory
    ): org.locationtech.jts.geom.LinearRing {
        this.coordinates

        return org.locationtech.jts.geom.LinearRing(
            CoordinateArraySequence(
                arrayOf(
                    this.coordinates[0].copy().apply {
                        x -= number
                        y -= number
                    },
                    this.coordinates[1].copy().apply {
                        x -= number
                        y += number
                    },
                    this.coordinates[2].copy().apply {
                        x += number
                        y += number
                    },
                    this.coordinates[3].copy().apply {
                        x += number
                        y -= number
                    },
                    this.coordinates[4].copy().apply {
                        x -= number
                        y -= number
                    }
                )
            ),
            geometryFactory
        )
    }
}