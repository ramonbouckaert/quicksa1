package io.bouckaert.quicksa1.shared.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object Roads: UUIDTable("roads") {
    val polygon = reference("polygon", MultiPolygons, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255).nullable()
}