package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.Blocks
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class Block(id: EntityID<UUID>): Entity<UUID>(id) {
    companion object : EntityClass<UUID, Block>(Blocks)
    var polygon by MultiPolygon referencedOn Blocks.polygon
    var type by Blocks.type
    var streetNumber by Blocks.streetNumber
}