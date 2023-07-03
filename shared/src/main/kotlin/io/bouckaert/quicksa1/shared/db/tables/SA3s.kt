package io.bouckaert.quicksa1.shared.db.tables

import io.bouckaert.quicksa1.shared.db.enums.State
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object SA3s: IntIdTable("sa3s") {
    val name = varchar("name", 255)
    val polygon = reference("polygon", MultiPolygons, onDelete = ReferenceOption.CASCADE)
    val sa4 = reference("sa4", SA4s)
    val state = enumerationByName("state", 3, State::class)
}