package io.bouckaert.quicksa1.shared.db.tables

import io.bouckaert.quicksa1.db.multiPolygon
import org.jetbrains.exposed.dao.id.UUIDTable

object MultiPolygons: UUIDTable("multipolygons") {
    val geometry = multiPolygon("geometry")
}