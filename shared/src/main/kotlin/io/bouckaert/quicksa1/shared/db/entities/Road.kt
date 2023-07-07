package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.Roads
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class Road(id: EntityID<UUID>): Entity<UUID>(id) {
    companion object : EntityClass<UUID, Road>(Roads)
    var polygon by MultiPolygon referencedOn Roads.polygon
    var name by Roads.name
}