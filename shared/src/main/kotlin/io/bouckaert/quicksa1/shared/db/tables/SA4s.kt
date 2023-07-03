package io.bouckaert.quicksa1.shared.db.tables

import io.bouckaert.quicksa1.shared.db.enums.State
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object SA4s: IdTable<Short>("sa4s") {
    override val id = short("id").entityId()
    val name = varchar("name", 255)
    val polygon = reference("polygon", MultiPolygons, onDelete = ReferenceOption.CASCADE)
    val state = enumerationByName("state", 3, State::class)

    override val primaryKey = PrimaryKey(id)
}