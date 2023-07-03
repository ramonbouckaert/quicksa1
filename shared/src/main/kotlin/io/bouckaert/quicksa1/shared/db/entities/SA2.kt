package io.bouckaert.quicksa1.shared.db.entities

import io.bouckaert.quicksa1.shared.db.tables.SA1s
import io.bouckaert.quicksa1.shared.db.tables.SA2s
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SA2(id: EntityID<Int>): Entity<Int>(id) {
    companion object : EntityClass<Int, SA2>(SA2s)
    var name by SA2s.name
    var polygon by MultiPolygon referencedOn SA2s.polygon
    val sa1s by SA1 referrersOn SA1s.sa2
    var sa3 by SA3 referencedOn SA2s.sa3
    var sa4 by SA4 referencedOn SA2s.sa4
}