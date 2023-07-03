package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.MultiPolygons
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class MultiPolygon(id: EntityID<UUID>): Entity<UUID>(id) {
    companion object : EntityClass<UUID, MultiPolygon>(MultiPolygons)
    var geometry by MultiPolygons.geometry
}