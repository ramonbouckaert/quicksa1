package io.bouckaert.quicksa1.shared.db.tables

import io.bouckaert.quicksa1.shared.db.enums.State
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object SA1s: LongIdTable("sa1s") {
    val polygon = reference("polygon", MultiPolygons, onDelete = ReferenceOption.CASCADE)
    val sa2 = reference("sa2", SA2s)
    val sa3 = reference("sa3", SA3s)
    val sa4 = reference("sa4", SA4s)
    val state = enumerationByName("state", 3, State::class)
}