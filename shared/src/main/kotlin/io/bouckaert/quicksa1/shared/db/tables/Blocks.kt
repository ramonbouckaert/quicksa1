package io.bouckaert.quicksa1.shared.db.tables

import io.bouckaert.quicksa1.shared.db.enums.BlockType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object Blocks: UUIDTable("blocks") {
    val polygon = SA1s.reference("polygon", MultiPolygons, onDelete = ReferenceOption.CASCADE)
    val type = enumerationByName<BlockType>("type", 16)
    val streetNumber = integer("street_number").nullable()
}