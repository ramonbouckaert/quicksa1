package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.SA1s
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SA1(id: EntityID<Long>): Entity<Long>(id) {
    companion object : EntityClass<Long, SA1>(SA1s)
    var polygon by MultiPolygon referencedOn SA1s.polygon
    var sa2 by SA2 referencedOn SA1s.sa2
    var sa3 by SA3 referencedOn SA1s.sa3
    var sa4 by SA4 referencedOn SA1s.sa4
}